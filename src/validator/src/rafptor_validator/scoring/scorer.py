"""Composite score computation and verdict selection."""

from __future__ import annotations

from ..metadata.models import TleValidationResult
from ..structural.models import PageValidationResult, TextValidationResult
from ..visual.models import VisualScore
from .models import QaScore, QaVerdict
from .thresholds import ScoringThresholds, ScoringWeights


def compute_qa_score(
    visual: VisualScore,
    text_validation: TextValidationResult,
    page_validation: PageValidationResult,
    tle_validation: TleValidationResult | None,
    weights: ScoringWeights | None = None,
    thresholds: ScoringThresholds | None = None,
) -> QaScore:
    w = weights or ScoringWeights()
    t = thresholds or ScoringThresholds()

    visual_score = visual.overall_ssim
    structural_score = text_validation.overall_match_ratio
    if not page_validation.pages_match:
        structural_score *= 0.5
    metadata_score = tle_validation.match_ratio if tle_validation else 1.0

    total_weight = w.visual + w.structural + w.metadata
    composite = (
        w.visual * visual_score
        + w.structural * structural_score
        + w.metadata * metadata_score
    ) / total_weight
    composite = max(0.0, min(1.0, composite))

    if composite >= t.accept:
        verdict = QaVerdict.ACCEPTED
    elif composite >= t.review:
        verdict = QaVerdict.NEEDS_REVIEW
    else:
        verdict = QaVerdict.REJECTED

    details = {
        "ssim_mean": round(visual_score, 4),
        "ssim_min_page": round(visual.min_page_ssim, 4),
        "ssim_worst_page": visual.worst_page_index,
        "text_match_ratio": round(text_validation.overall_match_ratio, 4),
        "pages_match": page_validation.pages_match,
        "tle_preserved": tle_validation.match_ratio if tle_validation else None,
        "tle_missing": tle_validation.missing_tle if tle_validation else [],
    }
    return QaScore(
        visual_score=visual_score,
        structural_score=structural_score,
        metadata_score=metadata_score,
        composite_score=composite,
        verdict=verdict,
        details=details,
    )
