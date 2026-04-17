from __future__ import annotations

import pytest

from rafptor_mapper.afp_font.models import GlyphEntry
from rafptor_mapper.metrics.width_calculator import widths_from_afp, widths_in_points


def test_widths_from_afp_returns_float_widths() -> None:
    glyphs = [
        GlyphEntry(gcgid="A", width=12, height=18, baseline_offset=0, bitmap=b""),
        GlyphEntry(gcgid="B", width=8, height=18, baseline_offset=0, bitmap=b""),
    ]
    widths = widths_from_afp(glyphs)
    assert widths == {"A": 12.0, "B": 8.0}


def test_widths_in_points_converts_correctly() -> None:
    widths = {"A": 240.0}
    result = widths_in_points(widths, 240)
    assert result == {"A": 72.0}


def test_widths_in_points_rejects_zero_resolution() -> None:
    with pytest.raises(ValueError):
        widths_in_points({"A": 100.0}, 0)
