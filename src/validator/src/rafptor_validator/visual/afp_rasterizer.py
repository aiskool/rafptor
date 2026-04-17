"""Reference-image management.

For the first release we do not rasterise AFP streams directly; we load
pre-rasterised PNGs from disk and, for bootstrapping, offer a helper to
rasterise a "gold standard" PDF produced by the Rafptor converter.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image

from .rasterizer import rasterize_pdf


def load_reference_images(reference_dir: Path) -> list[Image.Image]:
    """Load every ``page_NNN.png`` image from ``reference_dir`` sorted by index."""
    if not reference_dir.exists() or not reference_dir.is_dir():
        raise FileNotFoundError(f"reference directory missing: {reference_dir}")
    candidates = sorted(
        reference_dir.glob("page_*.png"),
        key=lambda p: int(p.stem.split("_")[1]),
    )
    if not candidates:
        raise FileNotFoundError(f"no page_*.png files in {reference_dir}")
    return [Image.open(path).convert("L") for path in candidates]


def create_reference_from_pdf(
    pdf_path: Path,
    output_dir: Path,
    dpi: int = 300,
) -> list[Path]:
    """Produce ``page_NNN.png`` reference images from a gold-standard PDF."""
    output_dir.mkdir(parents=True, exist_ok=True)
    pages = rasterize_pdf(pdf_path, dpi=dpi)
    written: list[Path] = []
    for index, page in enumerate(pages, start=1):
        path = output_dir / f"page_{index:03d}.png"
        page.save(str(path))
        written.append(path)
    return written
