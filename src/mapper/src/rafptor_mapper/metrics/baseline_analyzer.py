"""Detect the baseline of a font from its glyphs."""

from __future__ import annotations

import statistics

from ..afp_font.models import GlyphEntry


def estimate_baseline(glyphs: list[GlyphEntry]) -> float:
    """Median of the per-glyph declared baseline offset."""
    offsets = [g.baseline_offset for g in glyphs if g.height > 0]
    if not offsets:
        return 0.0
    return float(statistics.median(offsets))


def estimate_height(glyphs: list[GlyphEntry]) -> float:
    heights = [g.height for g in glyphs if g.height > 0]
    if not heights:
        return 0.0
    return float(max(heights))
