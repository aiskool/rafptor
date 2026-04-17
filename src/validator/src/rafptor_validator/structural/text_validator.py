"""Text-preservation validation.

We compare the text extracted from the PDF (via PyMuPDF) with the text that
the AFP parser surfaced per page. A full-fledged Longest Common Subsequence
is O(n^2) and overkill for the MVP; we use a linear "find-from-cursor"
heuristic that is well-behaved on text whose order is preserved.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

import fitz

from .models import TextValidationResult


def extract_pdf_text(pdf_path: Path) -> list[str]:
    """Return one text string per PDF page."""
    pages: list[str] = []
    with fitz.open(str(pdf_path)) as doc:
        for page in doc:
            pages.append(page.get_text("text"))
    return pages


def _normalise(text: str) -> str:
    return " ".join(text.split())


def _count_matching_chars(a: str, b: str) -> int:
    """Linear-time "find from cursor" alignment."""
    matching = 0
    cursor = 0
    for char in a:
        found = b.find(char, cursor)
        if found != -1:
            matching += 1
            cursor = found + 1
    return matching


def compare_text(
    afp_text_pages: list[str],
    pdf_text_pages: list[str],
) -> TextValidationResult:
    total_afp = 0
    total_match = 0
    page_results: list[dict[str, Any]] = []

    max_pages = max(len(afp_text_pages), len(pdf_text_pages))
    for i in range(max_pages):
        afp_text = _normalise(afp_text_pages[i]) if i < len(afp_text_pages) else ""
        pdf_text = _normalise(pdf_text_pages[i]) if i < len(pdf_text_pages) else ""
        matching = _count_matching_chars(afp_text, pdf_text)
        total_afp += len(afp_text)
        total_match += matching
        denominator = max(len(afp_text), 1)
        page_results.append(
            {
                "page": i + 1,
                "afp_chars": len(afp_text),
                "pdf_chars": len(pdf_text),
                "matching_chars": matching,
                "ratio": matching / denominator,
            }
        )

    overall = total_match / max(total_afp, 1) if total_afp > 0 else 1.0
    return TextValidationResult(
        overall_match_ratio=overall,
        total_afp_chars=total_afp,
        total_matching_chars=total_match,
        page_results=page_results,
    )
