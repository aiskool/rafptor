"""Turn per-glyph metric differences into scale/offset adjustments."""

from __future__ import annotations

from dataclasses import dataclass

_EPS = 1e-3


@dataclass
class SpacingAdjustment:
    scale_factor: float
    baseline_offset: float


def compute_adjustment(
    afp_widths: dict[str, float],
    ttf_widths: dict[str, float],
    afp_baseline: float,
    ttf_baseline: float,
) -> SpacingAdjustment:
    """Compute the scale + baseline offset needed to align TTF to AFP."""
    common = set(afp_widths) & set(ttf_widths)
    if not common:
        return SpacingAdjustment(1.0, 0.0)
    ratios = [
        afp_widths[c] / max(ttf_widths[c], _EPS)
        for c in common
        if ttf_widths.get(c, 0.0) > 0
    ]
    scale = sum(ratios) / len(ratios) if ratios else 1.0
    scale = max(0.1, min(scale, 10.0))
    offset = float(afp_baseline - ttf_baseline)
    return SpacingAdjustment(scale, offset)
