"""PDF/A conformance probing.

This is a light detection pass: we check whether an ``OutputIntents`` entry
is present on the document catalog and whether any XMP declares a PDF/A
part/conformance. It is not a substitute for veraPDF — full preflight lands
in Phase 4.
"""

from __future__ import annotations

import re
from pathlib import Path

import fitz

from .models import PdfAValidationResult


def validate_pdfa(pdf_path: Path) -> PdfAValidationResult:
    has_intent = False
    conformance: str | None = None
    with fitz.open(str(pdf_path)) as doc:
        catalog = doc.pdf_catalog()
        try:
            obj = doc.xref_object(catalog, compressed=False)
        except Exception:  # pragma: no cover - PyMuPDF shim
            obj = ""
        if obj and "OutputIntents" in obj:
            has_intent = True
        try:
            xmp_xml = doc.xref_xml_metadata()  # type: ignore[attr-defined]
        except Exception:  # pragma: no cover
            xmp_xml = None
        if xmp_xml:
            part_match = re.search(r"<pdfaid:part>(\d+)</pdfaid:part>", xmp_xml)
            conf_match = re.search(r"<pdfaid:conformance>(\w+)</pdfaid:conformance>", xmp_xml)
            if part_match and conf_match:
                conformance = f"PDF/A-{part_match.group(1)}{conf_match.group(1)}"
    return PdfAValidationResult(
        has_output_intent=has_intent,
        conformance_declared=conformance,
        passed=has_intent and conformance is not None,
    )
