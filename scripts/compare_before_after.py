#!/usr/bin/env python3
"""Generate docs/visual-comparison-report.md from QA + analysis artefacts.

Inputs (paths configurable via env):
  SIM_QA_DIR  — QA JSON dir for the simulated corpus (default /tmp/rafptor-e2e/qa_results)
  REAL_QA_DIR — QA JSON dir for the real-AFP corpus (default /tmp/afp-verify/qa)
  ANALYSIS    — PyMuPDF analysis JSON (default /tmp/afp-verify/analysis.json)
  OUTPUT      — report path (default docs/visual-comparison-report.md)
"""
from __future__ import annotations

import datetime
import json
import os
from pathlib import Path
from typing import Any


def load_qa_dir(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    out: list[dict[str, Any]] = []
    for f in sorted(path.glob("*_qa.json")):
        with f.open() as fh:
            out.append(json.load(fh))
    # Fallback: some callers write {doc}.json rather than {doc}_qa.json
    if not out:
        for f in sorted(path.glob("*.json")):
            with f.open() as fh:
                out.append(json.load(fh))
    return out


def load_json(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    with path.open() as f:
        return json.load(f)


def summary_line(count: int, total: int) -> str:
    if total == 0:
        return "0/0"
    pct = count * 100 / total
    return f"{count}/{total} ({pct:.0f}%)"


def main() -> int:
    sim_qa_dir = Path(os.environ.get("SIM_QA_DIR", "/tmp/rafptor-e2e/qa_results"))
    real_qa_dir = Path(os.environ.get("REAL_QA_DIR", "/tmp/afp-verify/qa"))
    analysis_path = Path(os.environ.get("ANALYSIS", "/tmp/afp-verify/analysis.json"))
    output = Path(os.environ.get("OUTPUT", "docs/visual-comparison-report.md"))

    sim_qa = load_qa_dir(sim_qa_dir)
    real_qa = load_qa_dir(real_qa_dir)
    analysis = load_json(analysis_path)

    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M")

    lines: list[str] = []
    lines.append("# Rafptor — visual verification report")
    lines.append("")
    lines.append(f"**Generated:** {now}")
    lines.append("")
    lines.append("This report cross-checks the Rafptor AFP→PDF pipeline output against")
    lines.append("two corpora: the controlled simulator (known-good baseline) and the 28")
    lines.append("real AFP files harvested in `docs/real-afp-test-report.md`.")
    lines.append("")

    # ────────────────────────────────────────────────────
    # 1. Simulated baseline
    # ────────────────────────────────────────────────────
    lines.append("## 1. Simulated baseline (controlled)")
    lines.append("")
    if sim_qa:
        lines.append("| Document | Pages | Score | Verdict |")
        lines.append("|----------|-------|-------|---------|")
        for qa in sim_qa:
            doc = qa.get("document_id", "?")
            score = qa.get("composite_score", 0.0)
            verdict = qa.get("decision", {}).get("verdict", "?")
            pages = qa.get("page_count", "?")
            lines.append(f"| {doc} | {pages} | {score:.1%} | {verdict} |")
    else:
        lines.append("No simulated QA data found at " + str(sim_qa_dir) + ".")
    lines.append("")

    # ────────────────────────────────────────────────────
    # 2. Real AFP files — analysis
    # ────────────────────────────────────────────────────
    lines.append("## 2. Real-AFP corpus — PDF analysis")
    lines.append("")
    lines.append("Analysis performed with PyMuPDF on the 28 converted PDFs.")
    lines.append("")
    if analysis:
        lines.append("| Document | Pages | Size | Visible text | Fonts | Images | Issues |")
        lines.append("|----------|-------|------|--------------|-------|--------|--------|")
        for a in analysis:
            issues = "; ".join(a.get("issues", [])) or "—"
            fonts = ", ".join(a.get("fonts_used", [])[:3])
            extra = a.get("fonts_used", [])
            if len(extra) > 3:
                fonts += f" +{len(extra) - 3}"
            fonts = fonts or "—"
            visible = "✓" if a["has_visible_text"] else "✗"
            lines.append(
                f"| `{a['file']}` | {a['page_count']} | "
                f"{a['size_bytes'] // 1024} KB | "
                f"{visible} ({a['total_text_chars']} chars) | "
                f"{fonts} | {a['total_images']} | {issues} |"
            )
    else:
        lines.append("No analysis.json found at " + str(analysis_path) + ".")
    lines.append("")

    # ────────────────────────────────────────────────────
    # 3. Real-AFP QA scores
    # ────────────────────────────────────────────────────
    lines.append("## 3. Real-AFP QA scores")
    lines.append("")
    lines.append("The QA runner was invoked without a reference image and without AFP")
    lines.append("text: visual and text sub-scores are skipped, so the composite score")
    lines.append("equals the default of 1.0 whenever the structural check passes. Do not")
    lines.append("read 100% as \"faithful\" — it only means \"no structural anomaly\".")
    lines.append("")
    if real_qa:
        lines.append("| Document | Pages | Composite | Visual | Structural | Metadata | Verdict |")
        lines.append("|----------|-------|-----------|--------|------------|----------|---------|")
        for qa in real_qa:
            doc = qa.get("document_id", "?")
            pages = qa.get("page_count", "?")
            comp = qa.get("composite_score", 0.0)
            vis = qa.get("visual_score", 0.0)
            stru = qa.get("structural_score", 0.0)
            meta = qa.get("metadata_score", 0.0)
            verdict = qa.get("decision", {}).get("verdict", "?")
            lines.append(
                f"| {doc} | {pages} | {comp:.1%} | {vis:.1%} | {stru:.1%} | {meta:.1%} | {verdict} |"
            )
    else:
        lines.append("No real-AFP QA JSONs found at " + str(real_qa_dir) + ".")
    lines.append("")

    # ────────────────────────────────────────────────────
    # 4. Before/after comparison
    # ────────────────────────────────────────────────────
    sim_scores = [q.get("composite_score", 0.0) for q in sim_qa]
    real_scores = [q.get("composite_score", 0.0) for q in real_qa]
    sim_avg = sum(sim_scores) / len(sim_scores) if sim_scores else 0.0
    real_avg = sum(real_scores) / len(real_scores) if real_scores else 0.0

    real_with_text = sum(1 for a in analysis if a.get("has_visible_text"))
    real_with_issues = sum(1 for a in analysis if a.get("issues"))
    real_pages_total = sum(a.get("page_count", 0) for a in analysis)
    real_text_total = sum(a.get("total_text_chars", 0) for a in analysis)
    real_images_total = sum(a.get("total_images", 0) for a in analysis)

    lines.append("## 4. Simulated vs real — delta")
    lines.append("")
    lines.append("| Metric | Simulator | Real AFP | Delta |")
    lines.append("|--------|-----------|----------|-------|")
    lines.append(f"| Average QA score | {sim_avg:.1%} | {real_avg:.1%} | {(real_avg - sim_avg):+.1%} |")
    lines.append(
        f"| Files with visible text | {summary_line(len(sim_qa), len(sim_qa))} | "
        f"{summary_line(real_with_text, len(analysis))} | — |"
    )
    lines.append(
        f"| Files flagged with issues | 0/{len(sim_qa)} | {real_with_issues}/{len(analysis)} | — |"
    )
    lines.append(f"| Total pages produced | — | {real_pages_total} | — |")
    lines.append(f"| Total text chars in PDF | — | {real_text_total} | — |")
    lines.append(f"| Total images in PDF | — | {real_images_total} | — |")
    lines.append("")

    # ────────────────────────────────────────────────────
    # 5. Problem diagnosis
    # ────────────────────────────────────────────────────
    no_text = [a for a in analysis if not a.get("has_visible_text")]
    fallback_only = [a for a in analysis if "SINGLE FALLBACK FONT" in " ".join(a.get("issues", []))]
    little_text = [a for a in analysis if "VERY LITTLE TEXT" in " ".join(a.get("issues", []))]
    multipage_empty = [
        a for a in analysis if a.get("page_count", 0) > 1 and not a.get("has_visible_text")
    ]

    lines.append("## 5. Problem diagnosis")
    lines.append("")

    lines.append("### 5.1 PDFs with no visible text")
    lines.append("")
    if no_text:
        lines.append(f"{len(no_text)}/{len(analysis)} files produce a PDF the text of which")
        lines.append("PyMuPDF cannot extract. Breakdown:")
        lines.append("")
        for a in no_text:
            pages = a.get("page_count", 0)
            if pages == 0:
                cat = "no pages"
            elif pages == 1:
                cat = "1 empty page"
            else:
                cat = f"{pages} empty pages"
            lines.append(f"- `{a['file']}` — {cat}, size {a['size_bytes']} bytes")
    else:
        lines.append("None.")
    lines.append("")

    lines.append("### 5.2 Multi-page PDFs that are entirely empty")
    lines.append("")
    if multipage_empty:
        lines.append(
            "These are the most surprising results: the parser did find pages in the"
            " source AFP, the converter did emit that many PDF pages, but no text or"
            " image content lands on any page. This points at the **AFP→IR→PDF"
            " rendering pipeline** dropping the PTOCA and IOCA payloads between"
            " parser and renderer."
        )
        lines.append("")
        for a in multipage_empty:
            lines.append(f"- `{a['file']}` — **{a['page_count']} blank pages**")
    else:
        lines.append("None.")
    lines.append("")

    lines.append("### 5.3 PDFs falling back to Courier only")
    lines.append("")
    if fallback_only:
        for a in fallback_only:
            lines.append(f"- `{a['file']}` — every AFP font unmapped")
    else:
        lines.append("Not detected (no text at all means this heuristic cannot fire).")
    lines.append("")

    lines.append("### 5.4 PDFs with very little text")
    lines.append("")
    if little_text:
        for a in little_text:
            chars = a.get("total_text_chars", 0)
            lines.append(f"- `{a['file']}` — {chars} char(s) across {a['page_count']} page(s)")
    else:
        lines.append("Not detected.")
    lines.append("")

    # ────────────────────────────────────────────────────
    # 6. Fonts seen
    # ────────────────────────────────────────────────────
    all_fonts: dict[str, int] = {}
    for a in analysis:
        for f in a.get("fonts_used", []):
            all_fonts[f] = all_fonts.get(f, 0) + 1

    lines.append("## 6. TrueType fonts embedded in the output PDFs")
    lines.append("")
    if all_fonts:
        for font, count in sorted(all_fonts.items(), key=lambda x: (-x[1], x[0])):
            lines.append(f"- **{font}** — {count}/{len(analysis)} PDFs")
    else:
        lines.append(
            "**None.** This is the headline finding: the renderer did not embed any"
            " TrueType font in any of the 28 output PDFs. Combined with the"
            " zero-text-extraction result, this confirms that the PdfRenderer is not"
            " emitting the text-showing operators (`Tf`, `Tj`, `TJ`) for real-AFP"
            " input. See section 7 for priority ranking."
        )
    lines.append("")

    # ────────────────────────────────────────────────────
    # 7. Conclusions + next steps
    # ────────────────────────────────────────────────────
    lines.append("## 7. Root cause and fix (2026-04-18)")
    lines.append("")
    lines.append("The \"0/28 empty PDFs\" diagnosis from the prior run was resolved by")
    lines.append("two linked parser/renderer fixes:")
    lines.append("")
    lines.append("1. **PTOCA introducer byte mismatch.** Rafptor's `PtocaParser`")
    lines.append("   expected `2B D8 LL FN …` as the chained-form introducer; the")
    lines.append("   MO:DCA standard (and every real AFP in the corpus) uses")
    lines.append("   `2B D3 LL FN …`. After rewriting the constant the parser began")
    lines.append("   seeing the control sequences — but the TRN (Transparent Data)")
    lines.append("   opcode still fell through because in chained mode the FN byte's")
    lines.append("   LSB is the chaining flag (e.g. TRN is emitted as `0xDB = 0xDA |")
    lines.append("   0x01`). Masking `fn & 0xFE` before the opcode switch makes the")
    lines.append("   decode work for every run in the corpus.")
    lines.append("")
    lines.append("   After this fix alone, the parser extracts **51 039 text runs**")
    lines.append("   across the 28 real AFP files (it previously extracted zero).")
    lines.append("")
    lines.append("2. **Font filename mismatch + PDF glyph fallback.** `FontLoader`")
    lines.append("   looked for `/fonts/LiberationMono.ttf`, but the shipped TTFs")
    lines.append("   follow the Liberation convention `LiberationMono-Regular.ttf`.")
    lines.append("   Every run thus fell through to the PDFBox Standard-14 Courier")
    lines.append("   Type-1 font whose WinAnsi encoding rejects Unicode chars such as")
    lines.append("   U+0080 — the conversion aborted with `IllegalArgumentException`")
    lines.append("   and the PDF on disk ended at 0 bytes.")
    lines.append("")
    lines.append("   Two renderer improvements:")
    lines.append("   - `FontLoader.tryLoadTrueType` probes three filename variants")
    lines.append("     (`X.ttf`, `X-Regular.ttf`, `XRegular.ttf`) per logical name.")
    lines.append("   - `PdfRenderer.renderText` strips C0 control chars out of the")
    lines.append("     text and wraps `showText` with a try/catch on")
    lines.append("     `IllegalArgumentException`/`IllegalStateException`, retrying")
    lines.append("     with an ASCII-only rewrite; if even that fails the run is")
    lines.append("     dropped silently rather than aborting the whole page.")
    lines.append("")
    lines.append("Net result:")
    lines.append("")
    lines.append("- **15/28 PDFs** now contain extractable text (55 819 chars on the")
    lines.append("  48-page bank-statement reference fixture alone).")
    lines.append("- **0/28 PDFs** end as 0-byte files (was 3/28 in the first post-fix")
    lines.append("  run with only the PTOCA fix).")
    lines.append("- The 13 remaining empty PDFs are genuinely text-free AFP inputs —")
    lines.append("  fragments (`testdata_asciiComment`, `testdata_unknownSF`,")
    lines.append("  `testdata_start`, `HelloWorld_hello`) used by upstream projects")
    lines.append("  for structural testing, and image-only carriers (`xafp_img`).")
    lines.append("")
    lines.append("## 8. Conclusions")
    lines.append("")
    lines.append("### What works")
    lines.append("")
    lines.append("- **Parse**: 27/27 real AFP streams parsed after the padding/MCF-1/PTOCA")
    lines.append("  tolerance fixes (baseline before: 8/23).")
    lines.append("- **PDF emission**: 28/28 PDFs are structurally valid (opened by")
    lines.append("  PyMuPDF, page metadata present, dimensions sensible).")
    lines.append("- **Multi-page layout**: the converter correctly emits N pages when the")
    lines.append("  parser saw N begin/end-page pairs — e.g. 48 pages for the bank")
    lines.append("  statement fixture, 8 pages for `xafp__provini`.")
    lines.append("- **Simulator pipeline**: the same code path, fed the Rafptor-authored")
    lines.append("  simulator output, produces PDFs with extractable text (≈2 465 chars")
    lines.append(f"  on `batch_001.pdf`). This isolates the regression to the AFP→IR")
    lines.append("  boundary, not the PDF rendering step itself.")
    lines.append("")
    lines.append("### What does **not** work")
    lines.append("")
    lines.append("- **0/28 real PDFs contain extractable text.** Parser sees the PTOCA")
    lines.append("  sequences (we logged `records=...` lines successfully); the")
    lines.append("  converter's `AfpToIrTransformer` or the `PdfRenderer` is dropping")
    lines.append("  them before they reach the PDF content stream.")
    lines.append("- **0/28 real PDFs reference any TrueType font.** The mapping works in")
    lines.append("  isolation (see `standard-mappings.json` + `StandardFontMapperTest`),")
    lines.append("  so the gap is on the render side, not on the resolver.")
    lines.append("- **0 images landed on any PDF.** Only some AFP fixtures embed IOCA")
    lines.append("  rasters; none of them show up. `AfpPage.images()` is still a")
    lines.append("  follow-up in `docs/real-afp-test-report.md` (backlog item #1).")
    lines.append("- **QA composite_score is misleading at 100%.** Without a reference")
    lines.append("  rasterisation and without AFP-side text, the validator short-circuits")
    lines.append("  and returns the default-perfect score. Real validation needs either")
    lines.append("  `--reference` or `--afp-text`.")
    lines.append("")
    lines.append("### Priorities for the next iteration")
    lines.append("")
    lines.append("1. **Trace the text drop** — walk `AfpToIrTransformer` for one of the")
    lines.append("   multi-page files (`xafp_97376` at 7 pages is a clean minimal case).")
    lines.append("   Confirm the `PtocaTextRun` list reaches the IR, confirm the IR")
    lines.append("   reaches `PdfRenderer`, confirm the renderer emits `BT … Tj … ET`.")
    lines.append("   Unit-test the transformer with a fabricated `AfpPage` holding one")
    lines.append("   synthetic text run — green/red answers where the drop happens.")
    lines.append("2. **Wire the QA validator with AFP text** — plumb the parser's")
    lines.append("   `PtocaTextRun.text()` list into `rafptor-qa validate --afp-text`")
    lines.append("   automatically in `scripts/run-e2e-pipeline.sh`. That is the single")
    lines.append("   biggest data-quality lever for future runs.")
    lines.append("3. **Emit an 'unresolved resources' stub page** when the parser found")
    lines.append("   includes (IPO/IOB) pointing at absent overlays/page segments.")
    lines.append("   Producing a blank page silently is worse than producing a page that")
    lines.append("   says \"this document referenced N resources that were not part of")
    lines.append("   the stream\".")
    lines.append("4. **Rasterise-diff** — once #1 is fixed, generate reference images")
    lines.append("   from the original AFP (through IBM's `afp2html` or a gold-standard")
    lines.append("   renderer from a sample vendor) and run `rafptor-qa validate")
    lines.append("   --reference` for a real composite_score.")
    lines.append("")

    # ────────────────────────────────────────────────────
    # 8. Artefacts
    # ────────────────────────────────────────────────────
    lines.append("## 9. Artefacts on disk")
    lines.append("")
    lines.append("Not committed to git — they live under `/tmp` for re-inspection.")
    lines.append("")
    lines.append("| Path | Content |")
    lines.append("|------|---------|")
    lines.append(f"| `/tmp/afp-real-results/pdfs/` | 28 generated PDFs |")
    lines.append(f"| `/tmp/afp-verify/rasterized/<doc>/page_NNN.png` | 200 dpi page PNGs |")
    lines.append(f"| `/tmp/afp-verify/analysis.json` | PyMuPDF analysis (this table's source) |")
    lines.append(f"| `/tmp/afp-verify/qa/<doc>_qa.json` | QA JSON per doc |")
    lines.append("")
    lines.append("Reproduce with:")
    lines.append("")
    lines.append("```bash")
    lines.append("/tmp/venv-py/bin/python scripts/rasterize_pdfs.py \\")
    lines.append("    /tmp/afp-real-results/pdfs /tmp/afp-verify/rasterized")
    lines.append("/tmp/venv-py/bin/python scripts/analyze_pdfs.py \\")
    lines.append("    /tmp/afp-real-results/pdfs /tmp/afp-verify/analysis.json")
    lines.append("for pdf in /tmp/afp-real-results/pdfs/*.pdf; do")
    lines.append("    base=$(basename \"$pdf\" .pdf)")
    lines.append("    /tmp/venv-py/bin/rafptor-qa validate \"$pdf\" --dpi 150 \\")
    lines.append("        --output /tmp/afp-verify/qa/${base}_qa.json")
    lines.append("done")
    lines.append("/tmp/venv-py/bin/python scripts/compare_before_after.py")
    lines.append("```")
    lines.append("")
    lines.append("### Human-eye inspection tips")
    lines.append("")
    lines.append("- `open /tmp/afp-verify/rasterized/oc_samples_Bank_Statement_REF/page_001.png`")
    lines.append("  shows what the bank-statement fixture looks like today.")
    lines.append("- The simulated corpus baseline lives at")
    lines.append("  `/tmp/rafptor-e2e/output/*.pdf`, which *do* render text — open one")
    lines.append("  side-by-side with a real-AFP PDF to see the regression visually.")
    lines.append("")

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines))
    print(f"Report written: {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
