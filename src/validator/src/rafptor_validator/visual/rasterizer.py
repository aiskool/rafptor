"""PDF page rasterisation via PyMuPDF.

Safety: the PDF size is capped at ``MAX_PDF_SIZE_BYTES`` and the page count
at ``MAX_PDF_PAGES`` before any allocation. Client content is never logged.
"""

from __future__ import annotations

from pathlib import Path

import fitz
from PIL import Image

from ..config import DEFAULT_DPI, MAX_PDF_PAGES, MAX_PDF_SIZE_BYTES


def rasterize_pdf(
    pdf_path: Path,
    dpi: int = DEFAULT_DPI,
    max_pages: int | None = None,
) -> list[Image.Image]:
    """Rasterise every page of a PDF file.

    Args:
        pdf_path: path to the PDF file.
        dpi: rendering resolution in dots per inch.
        max_pages: optional upper bound on the number of pages rendered.

    Returns:
        A list of PIL images (mode ``RGB``), one per page.
    """
    file_size = pdf_path.stat().st_size
    if file_size > MAX_PDF_SIZE_BYTES:
        raise ValueError(
            f"PDF too large: {file_size} bytes (max {MAX_PDF_SIZE_BYTES})"
        )
    if dpi <= 0:
        raise ValueError("dpi must be > 0")

    page_cap = max_pages if max_pages is not None else MAX_PDF_PAGES
    pages: list[Image.Image] = []

    with fitz.open(str(pdf_path)) as doc:
        if doc.page_count > MAX_PDF_PAGES:
            raise ValueError(
                f"PDF exceeds page limit: {doc.page_count} > {MAX_PDF_PAGES}"
            )
        zoom = dpi / 72.0
        matrix = fitz.Matrix(zoom, zoom)
        for i in range(min(doc.page_count, page_cap)):
            page = doc[i]
            pix = page.get_pixmap(matrix=matrix, alpha=False)
            img = Image.frombytes("RGB", (pix.width, pix.height), pix.samples)
            pages.append(img)
    return pages


def rasterize_pdf_to_grayscale(
    pdf_path: Path,
    dpi: int = DEFAULT_DPI,
    max_pages: int | None = None,
) -> list[Image.Image]:
    """Same as :func:`rasterize_pdf` but returns grayscale (``L``) images."""
    return [p.convert("L") for p in rasterize_pdf(pdf_path, dpi, max_pages)]
