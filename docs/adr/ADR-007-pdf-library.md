# ADR-007 — PDF library: Apache PDFBox vs iText

- **Status:** Accepted
- **Date:** 2026-04-16
- **Deciders:** Lead architect, converter team lead, legal advisor
- **Related modules:** converter (Module 5)

## Context and problem statement

Module 5 converts the parser's intermediate representation to PDF 1.7 and PDF/A-1b / PDF/A-3b. We need a Java library that produces standards-compliant PDFs, handles font embedding, colour profiles, positioned text, images, barcodes, and metadata. Rafptor is proprietary software sold to BFSI clients.

## Decision drivers

- **Licence compatibility** with a proprietary product
- **PDF/A conformance** — ISO 19005-1 (PDF/A-1b) and ISO 19005-3 (PDF/A-3b)
- **Maturity** — BFSI clients audit library choices
- **Feature parity** with AFP surface: positioned text, raster/vector images, barcodes, ICC profiles, XMP metadata
- **Community and security track record**

## Considered options

- Option A — **Apache PDFBox 3.x** (Apache-2.0)
- Option B — **iText 8 / 9** (AGPL OR commercial)
- Option C — **OpenPDF** (LGPL fork of iText 4, community-maintained)
- Option D — jPDFWriter (proprietary, commercial)

## Decision outcome

**Chosen: Apache PDFBox 3.x.**

PDFBox is Apache-2.0, compatible with proprietary licensing out of the box. It is mature, widely deployed in BFSI (including Adobe-equivalent usage), and supports every PDF feature we need. iText is disqualified due to AGPL contamination risk and the cost of the commercial licence.

## Pros and cons per option

### Option A — Apache PDFBox 3.x

- Good: Apache-2.0 — unambiguously compatible with proprietary product licensing.
- Good: 3.x release (2024) modernised the API; PDF/A-1b and PDF/A-3b supported via Preflight module and external veraPDF validation.
- Good: positioned text (`PDPageContentStream.setTextMatrix`), image embedding, barcode vector drawing all supported.
- Good: active community, regular security releases.
- Neutral: PDF/A generation requires careful construction; veraPDF used as CI validator.

### Option B — iText 8 / 9

- Good: arguably the most feature-rich PDF library.
- Good: explicit PDF/A-1b, PDF/A-2b, PDF/A-3b support.
- Bad: AGPL — any distribution of a derived work requires source disclosure under §13. Incompatible with proprietary product.
- Bad: commercial licence available but costs (confidentially) in the range of €20K–€100K/year depending on scope — meaningful sum on top of a pre-revenue project.
- Bad: legal risk if even transitive classpath inclusion happens by accident.

### Option C — OpenPDF

- Good: LGPL — usable in a proprietary product without source disclosure.
- Bad: forked from iText 4 (2015); missing modern features and security patches.
- Bad: smaller community; slower to adopt spec updates (PDF 2.0, PDF/A-3).
- Use: rejected as the primary library; kept as a potential fallback for legacy exotic features.

### Option D — Commercial libraries (jPDFWriter, PDFlib, etc.)

- Good: commercial support SLA.
- Bad: per-deployment licensing fees that don't scale to multi-client SaaS-like pricing.
- Bad: we become dependent on a single vendor for a core capability.

## Consequences

- **Short-term**: `src/converter/pom.xml` already pins `pdfbox:3.0.2`; `iText` and `OpenPDF` explicitly excluded.
- **CI**: veraPDF added to the Java CI pipeline to validate every generated PDF/A artefact.
- **Feature gap coverage**: if a specific AFP feature requires something PDFBox cannot produce natively, we build the low-level PDF content stream ourselves — still under Apache-2.0.
- **Licence scan**: `.github/workflows/ci.yml` licence-scan job explicitly blocks `AGPL-3.0` matches in manifests (current implementation) and will be hardened to use FOSSA / Syft SPDX detection before Phase 2 exit.

## Links

- `docs/development-plan.md` §3 Module 5
- [Apache PDFBox project](https://pdfbox.apache.org/)
- [veraPDF validator](https://verapdf.org/)
- [iText AGPL licensing FAQ](https://itextpdf.com/en/how-buy/faq)
