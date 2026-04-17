from __future__ import annotations

from rafptor_mapper.matching.metrics_matcher import compare_metrics


def test_identical_metrics_score_is_one() -> None:
    widths = {"A": 10.0, "B": 10.0}
    result = compare_metrics(widths, widths, 100, 100, 10, 10)
    assert result.score == 1.0
    assert result.monospace_match is True


def test_score_within_bounds() -> None:
    widths_a = {"A": 10.0}
    widths_b = {"A": 20.0}
    result = compare_metrics(widths_a, widths_b, 100, 200, 10, 20)
    assert 0.0 <= result.score <= 1.0


def test_no_common_chars_returns_zero_score() -> None:
    result = compare_metrics({"A": 1}, {"B": 1}, 10, 10, 1, 1)
    assert result.score == 0.0
