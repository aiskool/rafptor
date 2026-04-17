"""Rasterise TrueType glyphs for matching."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from ..afp_font.glyph_extractor import normalize_glyph
from ..config import DEFAULT_TTF_RENDER_SIZE, GLYPH_TARGET_SIZE


def render_glyph(
    ttf_path: Path,
    char: str,
    size_px: int = DEFAULT_TTF_RENDER_SIZE,
    target_size: int = GLYPH_TARGET_SIZE,
) -> Image.Image:
    if not char:
        raise ValueError("char must not be empty")
    font = ImageFont.truetype(str(ttf_path), size_px)
    try:
        bbox = font.getbbox(char)
    except Exception:  # pragma: no cover - fontTools/PIL compat shim
        bbox = None
    if bbox is None or bbox[2] <= bbox[0] or bbox[3] <= bbox[1]:
        return Image.new("L", (target_size, target_size), 255)
    w = bbox[2] - bbox[0]
    h = bbox[3] - bbox[1]
    img = Image.new("L", (w + 4, h + 4), 255)
    draw = ImageDraw.Draw(img)
    draw.text((-bbox[0] + 2, -bbox[1] + 2), char, font=font, fill=0)
    return normalize_glyph(img, target_size)


def render_charset(
    ttf_path: Path,
    chars: list[str],
    size_px: int = DEFAULT_TTF_RENDER_SIZE,
    target_size: int = GLYPH_TARGET_SIZE,
) -> dict[str, Image.Image]:
    return {c: render_glyph(ttf_path, c, size_px, target_size) for c in chars}
