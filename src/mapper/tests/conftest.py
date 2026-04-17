"""Shared fixtures for the mapper tests."""

from __future__ import annotations

from pathlib import Path

import pytest
from PIL import Image, ImageDraw


@pytest.fixture
def letter_image() -> Image.Image:
    img = Image.new("L", (32, 48), 255)
    draw = ImageDraw.Draw(img)
    draw.text((4, 4), "A", fill=0)
    return img


@pytest.fixture
def blank_image() -> Image.Image:
    return Image.new("L", (32, 48), 255)


@pytest.fixture
def tiny_afp_charset(tmp_path: Path) -> Path:
    """Write a minimal valid-looking character set to disk.

    The file is not a real FOCA character set — the reader treats unknown
    structured fields tolerantly, so the test only exercises the framing
    logic (length + id + payload).
    """
    data = bytearray()
    # Unknown SF: 0x5A | len=0x0009 | id=0xD3 0xFF 0xFF | flags+reserved | 1-byte payload
    data += bytes([0x5A, 0x00, 0x09, 0xD3, 0xFF, 0xFF, 0x00, 0x00, 0x00])
    data += bytes([0x00])
    path = tmp_path / "charset.bin"
    path.write_bytes(bytes(data))
    return path
