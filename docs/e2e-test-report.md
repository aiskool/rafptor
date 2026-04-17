# Rafptor — End-to-End Pipeline Test Report

**Date:** 2026-04-17
**Scope:** Collector (M3) → Parser (M1) → Mapper (M4) → Converter (M5) → Validator (M6)
**Runner:** `scripts/run-e2e-pipeline.sh`
**Test artefacts:** `/tmp/rafptor-e2e/`

## Executive summary

The full five-module pipeline runs end-to-end without manual intervention. A simulated bundle of 3 AFP documents (6 pages total, ~19 KB) flows through parser → converter → validator and lands as three PDFs (v1.6, 1.7–3.5 KB each) with a composite QA score of **1.00 (accepted)** across the board in bootstrap mode.

One binary format divergence was found and fixed in the simulator (MCF repeating-group framing). The mapper runs against the font stubs but reports all glyphs as "bitmap offset out of range" — expected, the simulator emits placeholder FOCA records rather than real rasters.

## Results per step

| Step | Module | Status | Notes |
|---|---|---|---|
| 1. Simulate | collector | ✅ | 3 streams, 2 fonts, 19 166 bytes, manifest VALID |
| 2. Parse   | parser Java | ✅ after fix | 11/17/23 records, 1/2/3 pages, 3 TLE tags per doc |
| 3. Map     | mapper Python | ⚠️ runs but warns | FOCA stubs → all glyph offsets out-of-range; documented |
| 4. Convert | converter Java | ✅ | 3 PDFs, SUCCESS status, 156/160/220 ms, 1.7–3.5 KB |
| 5. QA      | validator Python | ✅ | composite_score=1.0 on all 3 PDFs, verdict=accepted |

## Divergence found and fixed

### MCF (Map Coded Font) wire format — collector was missing the length-prefix byte

**Symptom.** Parser failed on every AFP stream with:
```
MalformedFieldException: MCF entry length 0 < 2 at offset 0
```

**Root cause.** `src/collector/src/rafptor_collector/generator/afp_stream.py::_make_mcf` emitted the MCF payload as `[local_id][0x00×3][charset][codepage]`. The parser (`src/parser/src/main/java/com/rafptor/parser/modca/MapCodedFont.java`) implements the **MCF-2 variant**, which expects each repeating group to start with a **1-byte length prefix** covering the RG itself + its body. The first byte of `0x00` (the local id) was therefore read as `rg_length = 0`, which is `< 2` — reject.

**Fix.** Commit re-frames the RG:
```python
body = [local_id] + charset_8B + codepage_8B  # 17 bytes
rg   = [1 + len(body)] + body                  # 18 bytes total
```
The extra 3 zero-bytes of the old format were meaningless padding; removing them matches the parser without breaking collector unit tests (25 passed).

## What the mapper warning means

`rafptor-mapper analyze C0H20000` returns JSON with a non-empty `warnings: ["glyph '…' bitmap offset out of range", …]`. This is expected: the simulator's FOCA writer produces a *stub* character set — a single unknown SF of 9 bytes — without real glyph rasters. The mapper's tolerant reader consumes it, falls back to zeroed metadata, and logs the offset-out-of-range warnings. The mapper exit code is 0, so the pipeline continues; this is a known "documented but deferred" point covered by the collector's `afp_stream.py` comment that FOCA output is a placeholder.

**Next step for the mapper path.** When the simulator grows a real FOCA emitter (or when we feed real client fonts), the mapper should be re-validated against the TTF catalogue with `rafptor-mapper match`.

## PDF sanity check

```
$ file /tmp/rafptor-e2e/output/*.pdf
batch_001.pdf: PDF document, version 1.6
batch_002.pdf: PDF document, version 1.6
batch_003.pdf: PDF document, version 1.6
```

All three open in Preview; the pages are rendered with the text runs positioned by the PTOCA parser → IR → PdfRenderer chain. Fidelity is not measured here (the AFP→IR mapping is best-effort and not glyph-accurate yet); the goal of this test was end-to-end wiring, which is achieved.

## QA score (bootstrap mode)

The baseline is created from the freshly generated PDFs themselves, so any non-1.0 score would indicate a non-determinism between the rasterizer's first and second pass. Score is exactly 1.0 on all three runs, which means the rasterizer is stable.

```json
{
  "composite_score": 1.0,
  "decision": { "verdict": "accepted", "action": "archive" },
  "visual_score": 1.0,
  "structural_score": 1.0,
  "metadata_score": 1.0,
  "warnings": ["No AFP text provided — text validation skipped."]
}
```

The warning `No AFP text provided — text validation skipped` is structural: the QA runner was invoked without the `--afp-text` flag, so text-preservation scoring is bypassed. Adding text-piping between parser (`PtocaTextRun.text()`) and validator is the next-most-useful integration.

## Artefacts committed to the repo

| Path | Purpose |
|---|---|
| `src/parser/src/main/java/com/rafptor/parser/E2EParseTest.java` | CLI runner that parses every `.afp` in a directory and dumps the AST summary |
| `src/converter/src/main/java/com/rafptor/converter/E2EConvertTest.java` | CLI runner that parses one AFP then calls `RafptorConverter.convert` |
| `scripts/run-e2e-pipeline.sh` | Single script that runs steps 1-5 and prints composite scores |
| `docs/e2e-test-report.md` | This report |

## Next steps identified

1. **Wire AFP text into the QA runner.** `rafptor-qa validate --afp-text <json>` should consume the per-page text from the parser so `text_match_ratio` becomes a real score.
2. **Replace FOCA stub with a real (even minimal) raster.** Even a 8×12 filled rectangle per glyph would let the mapper match against a TTF and produce a font mapping.
3. **Cross-language integration test.** Wrap `scripts/run-e2e-pipeline.sh` in a CI job (manual-dispatch only at first) so regressions in wire format are caught before a merge.
4. **Hermetic font resources.** Decide whether the converter should resolve `FONT_0` / `FONT_1` against the bundle's `resources/fonts/` or against an ICC-paired Liberation mirror on the classpath.
5. **PTOCA fidelity.** Verify that AMI/AMB (moves) and TRN (text) produce coordinates consistent with AFP's 1440-unit resolution → PDF points at the converter's DPI.

## Reproducing

```bash
# One-shot:
./scripts/run-e2e-pipeline.sh

# Or step by step (see the script for exact commands).
```

Pre-req: Homebrew `openjdk@17` + `maven`, the editable Python envs for collector/mapper/validator under `/tmp/venv-py/bin`.
