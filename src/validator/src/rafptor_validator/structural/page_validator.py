"""Page-count validation."""

from __future__ import annotations

from .models import PageValidationResult


def validate_page_count(afp_pages: int, pdf_pages: int) -> PageValidationResult:
    match = afp_pages == pdf_pages
    if match:
        message = f"Page count OK: {afp_pages} pages"
    else:
        message = (
            f"Page count MISMATCH: AFP has {afp_pages} pages, PDF has {pdf_pages} pages"
        )
    return PageValidationResult(
        afp_page_count=afp_pages,
        pdf_page_count=pdf_pages,
        pages_match=match,
        message=message,
    )
