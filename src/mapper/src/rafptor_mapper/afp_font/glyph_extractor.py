"""Turn AFP 1-bit bitmaps into normalised PIL images."""

from __future__ import annotations

import numpy as np
from PIL import Image

from ..config import GLYPH_TARGET_SIZE
from .models import GlyphEntry


def glyph_to_image(glyph: GlyphEntry) -> Image.Image:
    """Unpack a 1-bit MSB-first AFP bitmap into an ``L``-mode PIL image."""
    if glyph.width <= 0 or glyph.height <= 0:
        return Image.new("L", (1, 1), 255)
    row_bytes = (glyph.width + 7) // 8
    buffer = np.frombuffer(glyph.bitmap, dtype=np.uint8)
    expected = row_bytes * glyph.height
    if buffer.size < expected:
        buffer = np.pad(buffer, (0, expected - buffer.size), constant_values=0)
    else:
        buffer = buffer[:expected]
    bits = np.unpackbits(buffer).reshape(glyph.height, row_bytes * 8)
    bits = bits[:, : glyph.width]
    # AFP: 1 = ink (black). PIL L: 0 = black, 255 = white.
    pixels = ((1 - bits) * 255).astype(np.uint8)
    img: Image.Image = Image.fromarray(pixels, mode="L")  # type: ignore[no-untyped-call]
    return img


def normalize_glyph(img: Image.Image, target_size: int = GLYPH_TARGET_SIZE) -> Image.Image:
    """Resize proportionally to fit in a ``target_size × target_size`` white canvas."""
    if target_size <= 0:
        raise ValueError("target_size must be > 0")
    inner = max(1, target_size - 8)
    w, h = img.size
    if w == 0 or h == 0:
        return Image.new("L", (target_size, target_size), 255)
    scale = min(inner / w, inner / h)
    new_w = max(1, int(round(w * scale)))
    new_h = max(1, int(round(h * scale)))
    resized = img.resize((new_w, new_h), Image.Resampling.LANCZOS)
    canvas = Image.new("L", (target_size, target_size), 255)
    offset_x = (target_size - new_w) // 2
    offset_y = (target_size - new_h) // 2
    canvas.paste(resized, (offset_x, offset_y))
    return canvas


def extract_all_glyphs(
    glyphs: list[GlyphEntry], target_size: int = GLYPH_TARGET_SIZE
) -> dict[str, Image.Image]:
    """Rasterise and normalise every glyph from a character set."""
    result: dict[str, Image.Image] = {}
    for glyph in glyphs:
        result[glyph.gcgid] = normalize_glyph(glyph_to_image(glyph), target_size)
    return result
