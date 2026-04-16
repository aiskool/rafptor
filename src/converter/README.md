# Module 5 — AFP → PDF Conversion (Java)

Converts the parser's intermediate representation to PDF 1.7 / PDF/A-1b / PDF/A-3b using Apache PDFBox.

## Packages

| Path | Purpose |
|------|---------|
| `intermediate/` | IR schema (typed JSON/Protobuf) bridging parser output ↔ renderer input |
| `pdf/` | PDF rendering via Apache PDFBox (text runs, images, overlays, barcodes) |
| `archive/` | PDF/A-1b (ISO 19005-1) and PDF/A-3b compliance |

## Build

```bash
mvn clean verify
```

## Fidelity requirements

- Positioned text preserved pixel-accurate given the matched font (from Module 4).
- Overlays, form definitions, and page definitions faithfully reproduced.
- Barcodes (BCOCA) rendered as vector where possible; raster fallback only for legacy types.
- TLE / NOP metadata preserved in PDF document info and XMP metadata.

## Licensing note

- **PDFBox (Apache-2.0)**: compatible with proprietary licensing — used.
- **iText (AGPL / commercial)**: **not used** to avoid AGPL constraints; see ADR-007.

See also: `docs/development-plan.md` §3 Module 5.
