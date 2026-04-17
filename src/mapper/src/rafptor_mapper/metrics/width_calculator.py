"""Character-width helpers."""

from __future__ import annotations

from ..afp_font.models import GlyphEntry


def widths_from_afp(glyphs: list[GlyphEntry]) -> dict[str, float]:
    """Return each glyph's width in AFP L-units keyed by GCGID."""
    return {g.gcgid: float(g.width) for g in glyphs}


def widths_in_points(
    widths_lunits: dict[str, float], resolution_dpi: int
) -> dict[str, float]:
    if resolution_dpi <= 0:
        raise ValueError("resolution_dpi must be > 0")
    scale = 72.0 / float(resolution_dpi)
    return {gcgid: width * scale for gcgid, width in widths_lunits.items()}
