"""Tag Logical Element preservation check."""

from __future__ import annotations

import re
from pathlib import Path

import fitz

from .models import TleValidationResult


def _extract_pdf_metadata(pdf_path: Path) -> dict[str, str]:
    """Merge PDF info dict with any key found in the XMP payload."""
    merged: dict[str, str] = {}
    with fitz.open(str(pdf_path)) as doc:
        info = doc.metadata or {}
        for key, value in info.items():
            if value is None:
                continue
            merged[str(key)] = str(value)
        try:
            xmp_xml = doc.xref_xml_metadata()  # type: ignore[attr-defined]
        except Exception:  # pragma: no cover - PyMuPDF compat shim
            xmp_xml = None
        if xmp_xml:
            for match in re.finditer(r"<(\w+?):([\w\-]+)>([^<]+)</\1:\2>", xmp_xml):
                merged[match.group(2)] = match.group(3)
    return merged


def validate_tle(
    afp_tle: dict[str, str],
    pdf_path: Path,
    strict: bool = True,
) -> TleValidationResult:
    metadata = _extract_pdf_metadata(pdf_path)
    preserved = 0
    missing: list[str] = []
    for key, value in afp_tle.items():
        value_str = str(value)
        found = False
        for meta_key, meta_value in metadata.items():
            if key.lower() in meta_key.lower() and value_str in str(meta_value):
                found = True
                break
        if found:
            preserved += 1
        else:
            missing.append(key)
    total = len(afp_tle)
    ratio = preserved / total if total else 1.0
    passed = ratio >= (1.0 if strict else 0.8)
    return TleValidationResult(
        total_tle=total,
        preserved_tle=preserved,
        missing_tle=missing,
        match_ratio=ratio,
        passed=passed,
    )
