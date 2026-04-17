# Module 4 — Font Mapper (Python)

Matches AFP raster fonts to TrueType/OpenType candidates by comparing rendered glyphs and font metrics. Emits a JSON mapping that Module 5 (converter) consumes directly via `StandardFontMapper.java`.

## Architecture

```
AFP charset ─▶ reader (FOCA) ─▶ glyph_extractor ─┐
                                                  ├─▶ pixel_matcher (SSIM)
TTF catalogue ─▶ renderer ────────────────────────┤
                                                  ├─▶ metrics_matcher (widths/baseline)
widths + baseline ────────────────────────────────┤
                                                  ▼
                                     ensemble scorer ─▶ exporter (JSON)
```

## Packages

| Path | Role |
|------|------|
| `afp_font/` | Tolerant FOCA character-set reader + glyph rasterisation helpers |
| `ttf_font/` | TrueType renderer + directory catalogue with family/style heuristics |
| `matching/` | SSIM, metric comparison, ensemble scorer with confidence classification |
| `metrics/` | Character widths and baseline helpers |
| `export/` | JSON exporter compatible with `StandardFontMapper.java` + HTML report |
| `db/` | `InMemoryFontDatabase` (default) + lazy-loaded `MongoFontDatabase` |
| `cli.py` | `rafptor-mapper` entry point (analyze / match / batch / report) |

## CLI

```bash
rafptor-mapper analyze path/to/C0H20000
rafptor-mapper match    path/to/C0H20000 --catalog fonts/
rafptor-mapper batch    charsets/ --catalog fonts/ --output mapping.json --min-confidence medium
rafptor-mapper report   mapping.json --output mapping.html
```

Exit codes: `0` any candidate found, `2` empty result / fatal error.

## Build / test

```bash
make install        # pip install -e .[dev]
make lint
make typecheck
make test-cov       # ≥ 80 %
```

Local scaffold syntax check (no install):

```bash
python3 -c "import ast, glob; [ast.parse(open(p).read(), p) for p in glob.glob('src/mapper/src/**/*.py', recursive=True)]"
```

## Scoring

`combined = 0.40 × visual (SSIM) + 0.40 × metrics + 0.20 × character coverage`

| Combined | Confidence |
|----------|------------|
| ≥ 0.85 | high |
| ≥ 0.70 and < 0.85 | medium |
| < 0.70 | low |

Low-confidence results land in a `*.review.json` sibling file so humans can curate them before they reach production.

## Security posture

| Concern | Mitigation |
|---------|------------|
| Client glyph bytes in logs | Logger emits only GCGIDs, scores, counts — never raw glyph pixels |
| Oversized charset DoS | 10 MB hard cap, structured-field length bounds |
| Malformed FOCA | Tolerant parser; unknown fields counted as warnings, never fatal |
| Font licence contamination | Only Liberation (SIL OFL) / DejaVu / Noto documented in scripts/download_fonts.py. IBM proprietary fonts are never embedded. |

## Data

- `data/ibm_codepages/` ships CP500 and CP1140. Additional pages (CP1141/1147/1148) are documented in the directory's README. Use `scripts/` to regenerate when adding tables.
- `data/ttf_catalog/` is populated locally or by CI from the SIL OFL download locations listed in `scripts/download_fonts.py`.
- `data/glyph_cache/` is gitignored at the project level.

## Current limitations

- `feature_matcher.py` (CNN embedding-based matcher) is a stub. Phase 2 wires a small glyph CNN.
- FOCA field parsing uses common offsets (FNI record size 26 bytes) that work for the families we care about; exotic FOCA variants emit warnings but still load.
- Only two EBCDIC tables are bundled with the scaffold (cp500, cp1140). The remaining common pages are documented with recovery instructions.
- HTML report is intentionally minimal. Side-by-side visual comparisons land with Module 7 dashboard.
- Dependencies were reduced vs the original plan: no PyTorch, no OpenCV, no scikit-image — footprint minimal to keep the mapper deployable anywhere.

## References

- Wang, Bovik, Sheikh, Simoncelli — SSIM (2004)
- `docs/development-plan.md` §3 Module 4
- `docs/architecture/security-architecture.md` §4 Module 4 STRIDE
- `src/converter/src/main/resources/font-mappings/standard-mappings.json` (output schema contract)
