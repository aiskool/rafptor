# Rafptor — visual verification report

**Generated:** 2026-04-18 19:43

This report cross-checks the Rafptor AFP→PDF pipeline output against
two corpora: the controlled simulator (known-good baseline) and the 28
real AFP files harvested in `docs/real-afp-test-report.md`.

## 1. Simulated baseline (controlled)

| Document | Pages | Score | Verdict |
|----------|-------|-------|---------|
| batch_001 | 1 | 100.0% | accepted |
| batch_002 | 2 | 100.0% | accepted |
| batch_003 | 3 | 100.0% | accepted |

## 2. Real-AFP corpus — PDF analysis

Analysis performed with PyMuPDF on the 28 converted PDFs.

| Document | Pages | Size | Visible text | Fonts | Images | Issues |
|----------|-------|------|--------------|-------|--------|--------|
| `HelloWorld_hello.pdf` | 0 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only |
| `Sample_Files_Sample_1.pdf` | 1 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 1 page(s) |
| `Sample_Files_Sample_2.pdf` | 1 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 1 page(s) |
| `font_ttc.pdf` | 2 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 2 page(s) |
| `font_ttf.pdf` | 2 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 2 page(s) |
| `font_ttf_courier.pdf` | 2 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 2 page(s) |
| `health_01_Health_Coverage.pdf` | 1 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 1 page(s) |
| `oc_samples_Bank_Statement_REF.pdf` | 48 | 5 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 48 page(s) |
| `oc_samples_Letter_Ref.pdf` | 1 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 1 page(s) |
| `testdata_C0X00006.pdf` | 0 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only |
| `testdata_IPDSpan.pdf` | 0 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only |
| `testdata_asciiAndEbcdicComment.pdf` | 0 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only |
| `testdata_asciiComment.pdf` | 0 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only |
| `testdata_bim.pdf` | 0 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only |
| `testdata_cs.pdf` | 0 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only |
| `testdata_ende.pdf` | 1 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 1 page(s) |
| `testdata_fnirg10.pdf` | 0 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only |
| `testdata_repeatingGroupVariableLength.pdf` | 0 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only |
| `testdata_start.pdf` | 1 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 1 page(s) |
| `testdata_unknownSF.pdf` | 0 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only |
| `xafp_97376.pdf` | 7 | 1 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 7 page(s) |
| `xafp_X80_2C.pdf` | 1 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 1 page(s) |
| `xafp__provini.pdf` | 8 | 2 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 8 page(s) |
| `xafp__provini_1.pdf` | 8 | 2 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 8 page(s) |
| `xafp_fillet.pdf` | 5 | 1 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 5 page(s) |
| `xafp_img.pdf` | 2 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 2 page(s) |
| `xafp_original.pdf` | 4 | 1 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 4 page(s) |
| `xafp_x2.pdf` | 1 | 0 KB | ✗ (0 chars) | — | 0 | NO VISIBLE TEXT — PDF empty or image-only; VERY LITTLE TEXT — only 0 chars across 1 page(s) |

## 3. Real-AFP QA scores

The QA runner was invoked without a reference image and without AFP
text: visual and text sub-scores are skipped, so the composite score
equals the default of 1.0 whenever the structural check passes. Do not
read 100% as "faithful" — it only means "no structural anomaly".

| Document | Pages | Composite | Visual | Structural | Metadata | Verdict |
|----------|-------|-----------|--------|------------|----------|---------|
| HelloWorld_hello | 0 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| Sample_Files_Sample_1 | 1 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| Sample_Files_Sample_2 | 1 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| font_ttc | 2 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| font_ttf_courier | 2 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| font_ttf | 2 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| health_01_Health_Coverage | 1 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| oc_samples_Bank_Statement_REF | 48 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| oc_samples_Letter_Ref | 1 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| testdata_C0X00006 | 0 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| testdata_IPDSpan | 0 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| testdata_asciiAndEbcdicComment | 0 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| testdata_asciiComment | 0 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| testdata_bim | 0 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| testdata_cs | 0 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| testdata_ende | 1 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| testdata_fnirg10 | 0 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| testdata_repeatingGroupVariableLength | 0 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| testdata_start | 1 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| testdata_unknownSF | 0 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| xafp_97376 | 7 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| xafp_X80_2C | 1 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| xafp__provini_1 | 8 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| xafp__provini | 8 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| xafp_fillet | 5 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| xafp_img | 2 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| xafp_original | 4 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |
| xafp_x2 | 1 | 100.0% | 100.0% | 100.0% | 100.0% | accepted |

## 4. Simulated vs real — delta

| Metric | Simulator | Real AFP | Delta |
|--------|-----------|----------|-------|
| Average QA score | 100.0% | 100.0% | +0.0% |
| Files with visible text | 3/3 (100%) | 0/28 (0%) | — |
| Files flagged with issues | 0/3 | 28/28 | — |
| Total pages produced | — | 96 | — |
| Total text chars in PDF | — | 0 | — |
| Total images in PDF | — | 0 | — |

## 5. Problem diagnosis

### 5.1 PDFs with no visible text

28/28 files produce a PDF the text of which
PyMuPDF cannot extract. Breakdown:

- `HelloWorld_hello.pdf` — no pages, size 586 bytes
- `Sample_Files_Sample_1.pdf` — 1 empty page, size 741 bytes
- `Sample_Files_Sample_2.pdf` — 1 empty page, size 741 bytes
- `font_ttc.pdf` — 2 empty pages, size 849 bytes
- `font_ttf.pdf` — 2 empty pages, size 849 bytes
- `font_ttf_courier.pdf` — 2 empty pages, size 849 bytes
- `health_01_Health_Coverage.pdf` — 1 empty page, size 740 bytes
- `oc_samples_Bank_Statement_REF.pdf` — 48 empty pages, size 5713 bytes
- `oc_samples_Letter_Ref.pdf` — 1 empty page, size 766 bytes
- `testdata_C0X00006.pdf` — no pages, size 586 bytes
- `testdata_IPDSpan.pdf` — no pages, size 587 bytes
- `testdata_asciiAndEbcdicComment.pdf` — no pages, size 587 bytes
- `testdata_asciiComment.pdf` — no pages, size 586 bytes
- `testdata_bim.pdf` — no pages, size 586 bytes
- `testdata_cs.pdf` — no pages, size 586 bytes
- `testdata_ende.pdf` — 1 empty page, size 740 bytes
- `testdata_fnirg10.pdf` — no pages, size 587 bytes
- `testdata_repeatingGroupVariableLength.pdf` — no pages, size 587 bytes
- `testdata_start.pdf` — 1 empty page, size 740 bytes
- `testdata_unknownSF.pdf` — no pages, size 586 bytes
- `xafp_97376.pdf` — 7 empty pages, size 1376 bytes
- `xafp_X80_2C.pdf` — 1 empty page, size 740 bytes
- `xafp__provini.pdf` — 8 empty pages, size 2144 bytes
- `xafp__provini_1.pdf` — 8 empty pages, size 2052 bytes
- `xafp_fillet.pdf` — 5 empty pages, size 1169 bytes
- `xafp_img.pdf` — 2 empty pages, size 849 bytes
- `xafp_original.pdf` — 4 empty pages, size 1362 bytes
- `xafp_x2.pdf` — 1 empty page, size 740 bytes

### 5.2 Multi-page PDFs that are entirely empty

These are the most surprising results: the parser did find pages in the source AFP, the converter did emit that many PDF pages, but no text or image content lands on any page. This points at the **AFP→IR→PDF rendering pipeline** dropping the PTOCA and IOCA payloads between parser and renderer.

- `font_ttc.pdf` — **2 blank pages**
- `font_ttf.pdf` — **2 blank pages**
- `font_ttf_courier.pdf` — **2 blank pages**
- `oc_samples_Bank_Statement_REF.pdf` — **48 blank pages**
- `xafp_97376.pdf` — **7 blank pages**
- `xafp__provini.pdf` — **8 blank pages**
- `xafp__provini_1.pdf` — **8 blank pages**
- `xafp_fillet.pdf` — **5 blank pages**
- `xafp_img.pdf` — **2 blank pages**
- `xafp_original.pdf` — **4 blank pages**

### 5.3 PDFs falling back to Courier only

Not detected (no text at all means this heuristic cannot fire).

### 5.4 PDFs with very little text

- `Sample_Files_Sample_1.pdf` — 0 char(s) across 1 page(s)
- `Sample_Files_Sample_2.pdf` — 0 char(s) across 1 page(s)
- `font_ttc.pdf` — 0 char(s) across 2 page(s)
- `font_ttf.pdf` — 0 char(s) across 2 page(s)
- `font_ttf_courier.pdf` — 0 char(s) across 2 page(s)
- `health_01_Health_Coverage.pdf` — 0 char(s) across 1 page(s)
- `oc_samples_Bank_Statement_REF.pdf` — 0 char(s) across 48 page(s)
- `oc_samples_Letter_Ref.pdf` — 0 char(s) across 1 page(s)
- `testdata_ende.pdf` — 0 char(s) across 1 page(s)
- `testdata_start.pdf` — 0 char(s) across 1 page(s)
- `xafp_97376.pdf` — 0 char(s) across 7 page(s)
- `xafp_X80_2C.pdf` — 0 char(s) across 1 page(s)
- `xafp__provini.pdf` — 0 char(s) across 8 page(s)
- `xafp__provini_1.pdf` — 0 char(s) across 8 page(s)
- `xafp_fillet.pdf` — 0 char(s) across 5 page(s)
- `xafp_img.pdf` — 0 char(s) across 2 page(s)
- `xafp_original.pdf` — 0 char(s) across 4 page(s)
- `xafp_x2.pdf` — 0 char(s) across 1 page(s)

## 6. TrueType fonts embedded in the output PDFs

**None.** This is the headline finding: the renderer did not embed any TrueType font in any of the 28 output PDFs. Combined with the zero-text-extraction result, this confirms that the PdfRenderer is not emitting the text-showing operators (`Tf`, `Tj`, `TJ`) for real-AFP input. See section 7 for priority ranking.

## 7. Conclusions

### What works

- **Parse**: 27/27 real AFP streams parsed after the padding/MCF-1/PTOCA
  tolerance fixes (baseline before: 8/23).
- **PDF emission**: 28/28 PDFs are structurally valid (opened by
  PyMuPDF, page metadata present, dimensions sensible).
- **Multi-page layout**: the converter correctly emits N pages when the
  parser saw N begin/end-page pairs — e.g. 48 pages for the bank
  statement fixture, 8 pages for `xafp__provini`.
- **Simulator pipeline**: the same code path, fed the Rafptor-authored
  simulator output, produces PDFs with extractable text (≈2 465 chars
  on `batch_001.pdf`). This isolates the regression to the AFP→IR
  boundary, not the PDF rendering step itself.

### What does **not** work

- **0/28 real PDFs contain extractable text.** Parser sees the PTOCA
  sequences (we logged `records=...` lines successfully); the
  converter's `AfpToIrTransformer` or the `PdfRenderer` is dropping
  them before they reach the PDF content stream.
- **0/28 real PDFs reference any TrueType font.** The mapping works in
  isolation (see `standard-mappings.json` + `StandardFontMapperTest`),
  so the gap is on the render side, not on the resolver.
- **0 images landed on any PDF.** Only some AFP fixtures embed IOCA
  rasters; none of them show up. `AfpPage.images()` is still a
  follow-up in `docs/real-afp-test-report.md` (backlog item #1).
- **QA composite_score is misleading at 100%.** Without a reference
  rasterisation and without AFP-side text, the validator short-circuits
  and returns the default-perfect score. Real validation needs either
  `--reference` or `--afp-text`.

### Priorities for the next iteration

1. **Trace the text drop** — walk `AfpToIrTransformer` for one of the
   multi-page files (`xafp_97376` at 7 pages is a clean minimal case).
   Confirm the `PtocaTextRun` list reaches the IR, confirm the IR
   reaches `PdfRenderer`, confirm the renderer emits `BT … Tj … ET`.
   Unit-test the transformer with a fabricated `AfpPage` holding one
   synthetic text run — green/red answers where the drop happens.
2. **Wire the QA validator with AFP text** — plumb the parser's
   `PtocaTextRun.text()` list into `rafptor-qa validate --afp-text`
   automatically in `scripts/run-e2e-pipeline.sh`. That is the single
   biggest data-quality lever for future runs.
3. **Emit an 'unresolved resources' stub page** when the parser found
   includes (IPO/IOB) pointing at absent overlays/page segments.
   Producing a blank page silently is worse than producing a page that
   says "this document referenced N resources that were not part of
   the stream".
4. **Rasterise-diff** — once #1 is fixed, generate reference images
   from the original AFP (through IBM's `afp2html` or a gold-standard
   renderer from a sample vendor) and run `rafptor-qa validate
   --reference` for a real composite_score.

## 8. Artefacts on disk

Not committed to git — they live under `/tmp` for re-inspection.

| Path | Content |
|------|---------|
| `/tmp/afp-real-results/pdfs/` | 28 generated PDFs |
| `/tmp/afp-verify/rasterized/<doc>/page_NNN.png` | 200 dpi page PNGs |
| `/tmp/afp-verify/analysis.json` | PyMuPDF analysis (this table's source) |
| `/tmp/afp-verify/qa/<doc>_qa.json` | QA JSON per doc |

Reproduce with:

```bash
/tmp/venv-py/bin/python scripts/rasterize_pdfs.py \
    /tmp/afp-real-results/pdfs /tmp/afp-verify/rasterized
/tmp/venv-py/bin/python scripts/analyze_pdfs.py \
    /tmp/afp-real-results/pdfs /tmp/afp-verify/analysis.json
for pdf in /tmp/afp-real-results/pdfs/*.pdf; do
    base=$(basename "$pdf" .pdf)
    /tmp/venv-py/bin/rafptor-qa validate "$pdf" --dpi 150 \
        --output /tmp/afp-verify/qa/${base}_qa.json
done
/tmp/venv-py/bin/python scripts/compare_before_after.py
```

### Human-eye inspection tips

- `open /tmp/afp-verify/rasterized/oc_samples_Bank_Statement_REF/page_001.png`
  shows what the bank-statement fixture looks like today.
- The simulated corpus baseline lives at
  `/tmp/rafptor-e2e/output/*.pdf`, which *do* render text — open one
  side-by-side with a real-AFP PDF to see the regression visually.
