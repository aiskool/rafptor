"""Combine visual, metrics and coverage scores into a single ranking."""

from __future__ import annotations

from typing import TYPE_CHECKING

from ..config import CONFIDENCE_HIGH, CONFIDENCE_MEDIUM, DEFAULT_WEIGHTS
from .metrics_matcher import compare_metrics
from .models import MatchResult
from .pixel_matcher import compare_charsets

if TYPE_CHECKING:
    from PIL.Image import Image


def compute_combined_score(
    visual_score: float,
    metrics_score: float,
    char_coverage: float,
    weights: dict[str, float] | None = None,
) -> float:
    w = weights or DEFAULT_WEIGHTS
    total = w["visual"] + w["metrics"] + w["coverage"]
    if total <= 0:
        return 0.0
    combined = (
        w["visual"] * visual_score
        + w["metrics"] * metrics_score
        + w["coverage"] * char_coverage
    ) / total
    return max(0.0, min(1.0, combined))


def classify_confidence(score: float) -> str:
    if score >= CONFIDENCE_HIGH:
        return "high"
    if score >= CONFIDENCE_MEDIUM:
        return "medium"
    return "low"


def find_best_match(
    afp_font_name: str,
    afp_glyphs: dict[str, "Image"],
    afp_widths: dict[str, float],
    afp_height: float,
    afp_baseline: float,
    candidates: list[dict],
) -> list[MatchResult]:
    """Return candidates sorted by combined score (best first).

    Each candidate dict is::

        {
            "name": str,
            "path": str,
            "glyphs": dict[str, Image],
            "widths": dict[str, float],
            "height": float,
            "baseline": float,
        }
    """
    results: list[MatchResult] = []
    for cand in candidates:
        cand_glyphs = cand.get("glyphs", {})
        cand_widths = cand.get("widths", {})
        visual = compare_charsets(afp_glyphs, cand_glyphs)
        metrics = compare_metrics(
            afp_widths,
            cand_widths,
            afp_height=afp_height,
            ttf_height=cand.get("height", 0.0),
            afp_baseline=afp_baseline,
            ttf_baseline=cand.get("baseline", 0.0),
        )
        coverage = (
            len(set(afp_glyphs) & set(cand_glyphs)) / max(len(afp_glyphs), 1)
            if afp_glyphs
            else 0.0
        )
        combined = compute_combined_score(visual, metrics.score, coverage)
        warnings: list[str] = []
        if coverage < 0.95 and afp_glyphs:
            warnings.append(
                f"Missing {len(set(afp_glyphs) - set(cand_glyphs))} character(s) in candidate"
            )
        if metrics.max_width_diff > 0.1:
            warnings.append(
                f"Max char width difference: {metrics.max_width_diff:.1%}"
            )
        results.append(
            MatchResult(
                afp_font_name=afp_font_name,
                ttf_font_name=cand["name"],
                ttf_font_path=str(cand["path"]),
                visual_score=visual,
                metrics_score=metrics.score,
                combined_score=combined,
                confidence=classify_confidence(combined),
                char_coverage=coverage,
                warnings=warnings,
            )
        )
    results.sort(key=lambda r: r.combined_score, reverse=True)
    return results
