"""Shared fixtures: PIL-generated reference/test images and temp directories."""

from __future__ import annotations

from pathlib import Path

import pytest
from PIL import Image, ImageDraw


@pytest.fixture
def reference_image() -> Image.Image:
    img = Image.new("L", (400, 550), 255)
    draw = ImageDraw.Draw(img)
    for i in range(10):
        y = 40 + i * 40
        draw.text((30, y), f"Line {i + 1}: Rafptor QA reference sample.", fill=0)
    return img


@pytest.fixture
def test_image_similar(reference_image: Image.Image) -> Image.Image:
    img = reference_image.copy()
    px = img.load()
    # Single-pixel mutation to keep SSIM very high but not exactly 1.0.
    px[200, 200] = 100
    return img


@pytest.fixture
def test_image_different() -> Image.Image:
    img = Image.new("L", (400, 550), 255)
    draw = ImageDraw.Draw(img)
    for i in range(10):
        y = 60 + i * 45
        draw.text((80, y), f"Totally different content line {i}.", fill=0)
    return img


@pytest.fixture
def reference_dir(tmp_path: Path, reference_image: Image.Image) -> Path:
    out = tmp_path / "reference"
    out.mkdir()
    reference_image.save(str(out / "page_001.png"))
    return out
