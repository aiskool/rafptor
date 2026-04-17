from __future__ import annotations

from PIL import Image

from rafptor_mapper.matching.ensemble import (
    classify_confidence,
    compute_combined_score,
    find_best_match,
)


def test_compute_combined_score_bounds() -> None:
    assert compute_combined_score(0.0, 0.0, 0.0) == 0.0
    assert compute_combined_score(1.0, 1.0, 1.0) == 1.0


def test_classify_confidence_thresholds() -> None:
    assert classify_confidence(0.9) == "high"
    assert classify_confidence(0.75) == "medium"
    assert classify_confidence(0.5) == "low"


def test_best_match_ordering() -> None:
    img = Image.new("L", (32, 32), 255)
    afp_glyphs = {"A": img}
    candidates = [
        {
            "name": "weak",
            "path": "/fake/weak.ttf",
            "glyphs": {"A": Image.new("L", (32, 32), 0)},
            "widths": {"A": 100.0},
            "height": 100.0,
            "baseline": 0.0,
        },
        {
            "name": "strong",
            "path": "/fake/strong.ttf",
            "glyphs": {"A": img},
            "widths": {"A": 10.0},
            "height": 10.0,
            "baseline": 0.0,
        },
    ]
    results = find_best_match(
        afp_font_name="demo",
        afp_glyphs=afp_glyphs,
        afp_widths={"A": 10.0},
        afp_height=10.0,
        afp_baseline=0.0,
        candidates=candidates,
    )
    assert results[0].ttf_font_name == "strong"
