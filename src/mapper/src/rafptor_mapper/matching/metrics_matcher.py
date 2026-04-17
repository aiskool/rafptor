"""Character-width/height/baseline based comparison."""

from __future__ import annotations

from .models import FontMetricsComparison

_EPS = 1e-3


def _monospace(widths: dict[str, float]) -> bool:
    distinct = {round(w, 2) for w in widths.values() if w > 0}
    return len(distinct) <= 2


def compare_metrics(
    afp_widths: dict[str, float],
    ttf_widths: dict[str, float],
    afp_height: float,
    ttf_height: float,
    afp_baseline: float,
    ttf_baseline: float,
) -> FontMetricsComparison:
    common = set(afp_widths) & set(ttf_widths)
    if not common:
        return FontMetricsComparison(1.0, 1.0, 1.0, 1.0, False, 0.0)
    width_diffs = [
        abs(afp_widths[c] - ttf_widths[c]) / max(afp_widths[c], _EPS)
        for c in common
    ]
    avg_width = sum(width_diffs) / len(width_diffs)
    max_width = max(width_diffs)
    height_diff = abs(afp_height - ttf_height) / max(afp_height, _EPS)
    baseline_diff = abs(afp_baseline - ttf_baseline) / max(afp_baseline, _EPS)
    mono_match = _monospace(afp_widths) == _monospace(ttf_widths)
    penalty = (
        0.4 * min(avg_width, 1.0)
        + 0.2 * min(max_width, 1.0)
        + 0.2 * min(height_diff, 1.0)
        + 0.1 * min(baseline_diff, 1.0)
        + 0.1 * (0.0 if mono_match else 1.0)
    )
    score = max(0.0, min(1.0, 1.0 - penalty))
    return FontMetricsComparison(
        avg_width_diff=avg_width,
        max_width_diff=max_width,
        height_diff=height_diff,
        baseline_diff=baseline_diff,
        monospace_match=mono_match,
        score=score,
    )
