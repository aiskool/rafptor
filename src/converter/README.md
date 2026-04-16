# Module 5 — AFP → PDF Converter (Java 17+)

Consumes an `AfpDocument` produced by `rafptor-parser` (Module 1) and generates a PDF 1.7 or PDF/A-1b file via Apache PDFBox 3.x.

## Pipeline

```
AfpDocument ─▶ AfpToIrTransformer ─▶ IrDocument ─▶ PdfRenderer ─▶ PDF file
                                                         │
                                                         └─▶ PdfACompliance (optional)
                                                         └─▶ MetadataRenderer (always)
```

## Packages

| Package | Role |
|---------|------|
| `ir/` | Intermediate representation (IrDocument / IrPage / IrTextBlock / IrImage / IrGraphic / IrBarcode / IrOverlay / IrMetadata) |
| `transform/` | AfpDocument → IR (text transformer complete; image / graphic / barcode / overlay are stubs that surface warnings) |
| `font/` | JSON-loaded font mapping registry (Liberation fonts by default) + EBCDIC decoder delegate |
| `render/` | PDFBox orchestration: text, images, graphics, metadata, PDF/A-1b compliance |
| `validation/` | Post-conversion integrity check (page count, size limits) |

## Build

```bash
# Parser must be installed first (single-pom layout, no reactor yet)
cd src/parser && mvn -q install
cd ../converter && mvn -q verify
```

CI does this automatically — see `.github/workflows/ci.yml`.

## Coordinate system

AFP: origin top-left, Y grows downward, unit = L-unit at page resolution (240 dpi by default).
PDF: origin bottom-left, Y grows upward, unit = point (72 dpi).

Conversion:

- `ir_x_pt = afp_l_units * 72 / resolution`
- `ir_y_pt = afp_l_units * 72 / resolution`
- At render time: `pdf_y = pageHeight - ir_y - fontSize` (text baseline adjustment).

## Security posture

| Concern | Mitigation |
|---------|------------|
| Client content in logs | Logger emits structural counters only. Exception messages are truncated to 120 chars and stripped of stack traces before being surfaced in `ConversionResult.error`. |
| Image allocation attack | `ImageIO.read` followed by a 50 Mpix guard before `LosslessFactory.createFromImage`. Oversized images are dropped with a warning. |
| Output disk bomb | `ConversionConfig.maxOutputBytes` (default 1 GB) checked in `ConversionValidator`. |
| Path traversal | Caller responsibility — outputPath is written as-is via `Files.newOutputStream`. Sandbox at the pipeline layer. |
| Font licence contamination | `standard-mappings.json` maps to Liberation fonts (SIL OFL) and falls back to PDFBox standard-14 (Apache). No IBM proprietary font is ever embedded. |
| PDF/A non-compliance masquerading as compliance | `PdfACompliance.apply` emits an explicit warning when the ICC profile is missing — `status` becomes `WARNING`, not `SUCCESS`. |

## Current limitations

- Image / graphic / barcode transformers are stubs. They emit warnings and drop content. Full IOCA (FS10 bi-level, FS11 grayscale, FS42 JPEG) / GOCA / BCOCA implementation lands in Phase 2.
- MCF → code-page resolution is not wired yet; every text run uses the default mapping. The `FontMapper` contract is in place to consume the mapping once the parser surfaces it.
- Liberation `.ttf` and sRGB `.icc` are not committed. CI fetches them from a cached location; locally the renderer falls back to PDFBox standard-14 fonts.
- Preflight validation is not invoked (dependency declared; hook will arrive with Task 2.5 — "M2.2 veraPDF validation").
- No multi-module Maven reactor yet; converter depends on the parser as an installed artefact.

## References

- `docs/development-plan.md` §3 Module 5
- `docs/adr/ADR-007-pdf-library.md`
- [Apache PDFBox documentation](https://pdfbox.apache.org/3.0/)
- [ISO 19005-1 (PDF/A-1b)](https://www.iso.org/standard/38920.html)
