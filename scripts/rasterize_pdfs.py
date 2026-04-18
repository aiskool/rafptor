#!/usr/bin/env python3
"""Rasterise every PDF page to PNG for visual inspection.

Usage:
    scripts/rasterize_pdfs.py [pdf_dir] [output_base]

Defaults: /tmp/afp-real-results/pdfs → /tmp/afp-verify/rasterized
"""
from __future__ import annotations

import sys
from pathlib import Path

try:
    import fitz  # PyMuPDF
except ImportError:
    import subprocess

    subprocess.check_call([sys.executable, "-m", "pip", "install", "PyMuPDF"])
    import fitz  # noqa: E402


def rasterize(pdf_path: Path, output_dir: Path, dpi: int = 200) -> list[Path]:
    doc = fitz.open(str(pdf_path))
    output_dir.mkdir(parents=True, exist_ok=True)
    zoom = dpi / 72.0
    matrix = fitz.Matrix(zoom, zoom)
    pages: list[Path] = []
    for i in range(doc.page_count):
        page = doc[i]
        pix = page.get_pixmap(matrix=matrix, alpha=False)
        img_path = output_dir / f"page_{i + 1:03d}.png"
        pix.save(str(img_path))
        pages.append(img_path)
    doc.close()
    return pages


def main() -> int:
    pdf_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("/tmp/afp-real-results/pdfs")
    output_base = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("/tmp/afp-verify/rasterized")

    if not pdf_dir.exists():
        print(f"PDF directory not found: {pdf_dir}", file=sys.stderr)
        return 1

    pdfs = sorted(pdf_dir.glob("*.pdf"))
    print(f"Rasterising {len(pdfs)} PDFs → {output_base}\n")
    for pdf in pdfs:
        out_dir = output_base / pdf.stem
        pages = rasterize(pdf, out_dir)
        print(f"  {pdf.name:55s} {len(pages):2d} page(s)")

    print(f"\nDone. Open e.g. {output_base}/oc_samples_Bank_Statement_REF/page_001.png")
    return 0


if __name__ == "__main__":
    sys.exit(main())
