from __future__ import annotations

from pathlib import Path

import pytest

from rafptor_validator.visual.rasterizer import rasterize_pdf


def test_rejects_missing_file(tmp_path: Path) -> None:
    with pytest.raises(FileNotFoundError):
        rasterize_pdf(tmp_path / "missing.pdf")


def test_rejects_zero_dpi(tmp_path: Path) -> None:
    pdf = tmp_path / "empty.pdf"
    pdf.write_bytes(b"%PDF-1.4\n%%EOF\n")
    with pytest.raises(ValueError):
        rasterize_pdf(pdf, dpi=0)
