from __future__ import annotations

from rafptor_validator.metadata.models import TleValidationResult
from rafptor_validator.scoring.models import QaVerdict
from rafptor_validator.scoring.scorer import compute_qa_score
from rafptor_validator.scoring.thresholds import ScoringThresholds
from rafptor_validator.structural.models import PageValidationResult, TextValidationResult
from rafptor_validator.visual.models import VisualScore


def _visual(score: float) -> VisualScore:
    return VisualScore(
        overall_ssim=score,
        min_page_ssim=score,
        max_page_ssim=score,
        total_diff_pixels=0,
        avg_diff_percentage=0.0,
    )


def _text(ratio: float) -> TextValidationResult:
    return TextValidationResult(
        overall_match_ratio=ratio,
        total_afp_chars=10,
        total_matching_chars=int(10 * ratio),
    )


def _pages(match: bool) -> PageValidationResult:
    return PageValidationResult(
        afp_page_count=1,
        pdf_page_count=1 if match else 2,
        pages_match=match,
        message="",
    )


def _tle(ratio: float) -> TleValidationResult:
    return TleValidationResult(
        total_tle=1,
        preserved_tle=int(ratio),
        match_ratio=ratio,
        passed=ratio >= 1.0,
    )


def test_perfect_scores_are_accepted() -> None:
    score = compute_qa_score(_visual(1.0), _text(1.0), _pages(True), _tle(1.0))
    assert score.verdict is QaVerdict.ACCEPTED
    assert score.composite_score == 1.0


def test_low_visual_falls_to_review() -> None:
    score = compute_qa_score(_visual(0.75), _text(1.0), _pages(True), _tle(1.0))
    assert score.verdict is QaVerdict.NEEDS_REVIEW


def test_very_low_scores_rejected() -> None:
    score = compute_qa_score(_visual(0.3), _text(0.3), _pages(False), _tle(0.0))
    assert score.verdict is QaVerdict.REJECTED


def test_page_mismatch_halves_structural() -> None:
    good = compute_qa_score(_visual(1.0), _text(1.0), _pages(True), _tle(1.0))
    bad = compute_qa_score(_visual(1.0), _text(1.0), _pages(False), _tle(1.0))
    assert bad.composite_score < good.composite_score


def test_custom_thresholds_are_respected() -> None:
    score = compute_qa_score(
        _visual(0.85), _text(0.85), _pages(True), _tle(0.85),
        thresholds=ScoringThresholds(accept=0.80, review=0.50),
    )
    assert score.verdict is QaVerdict.ACCEPTED
