from __future__ import annotations

from rafptor_mapper.afp_font.glyph_extractor import glyph_to_image, normalize_glyph
from rafptor_mapper.afp_font.models import GlyphEntry


def test_bitmap_to_image_dimensions() -> None:
    glyph = GlyphEntry(gcgid="X", width=8, height=4, baseline_offset=0, bitmap=bytes(4))
    img = glyph_to_image(glyph)
    assert img.size == (8, 4)


def test_zero_sized_glyph_returns_white_pixel() -> None:
    glyph = GlyphEntry(gcgid="X", width=0, height=0, baseline_offset=0, bitmap=b"")
    img = glyph_to_image(glyph)
    assert img.size == (1, 1)
    assert img.getpixel((0, 0)) == 255


def test_normalise_centres_glyph() -> None:
    from PIL import Image

    src = Image.new("L", (10, 20), 0)
    out = normalize_glyph(src, target_size=64)
    assert out.size == (64, 64)
    assert out.getpixel((0, 0)) == 255  # white background preserved
