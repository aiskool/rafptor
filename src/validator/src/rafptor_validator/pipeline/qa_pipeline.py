"""Full QA pipeline orchestration."""

from __future__ import annotations

import json
import logging
import time
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any

from PIL.Image import Image as PilImage

from ..config import DEFAULT_DPI
from ..metadata.models import TleValidationResult
from ..metadata.tle_validator import validate_tle
from ..scoring.models import QaDecision, QaVerdict
from ..scoring.router import route_document
from ..scoring.scorer import compute_qa_score
from ..scoring.thresholds import ScoringThresholds, ScoringWeights
from ..structural.models import TextValidationResult
from ..structural.page_validator import validate_page_count
from ..structural.text_validator import compare_text, extract_pdf_text
from ..visual.afp_rasterizer import load_reference_images
from ..visual.comparator import compare_page
from ..visual.models import VisualScore
from ..visual.rasterizer import rasterize_pdf_to_grayscale

logger = logging.getLogger(__name__)


@dataclass
class QaPipelineResult:
    document_id: str
    decision: QaDecision
    visual_score: float
    structural_score: float
    metadata_score: float
    composite_score: float
    page_count: int
    duration_ms: int
    warnings: list[str] = field(default_factory=list)

    def to_json(self) -> str:
        payload: dict[str, Any] = {
            "document_id": self.document_id,
            "decision": {
                "document_id": self.decision.document_id,
                "verdict": self.decision.verdict.value,
                "composite_score": self.decision.composite_score,
                "pdf_path": str(self.decision.pdf_path),
                "timestamp": self.decision.timestamp.isoformat(),
                "action": self.decision.action,
                "reason": self.decision.reason,
            },
            "visual_score": self.visual_score,
            "structural_score": self.structural_score,
            "metadata_score": self.metadata_score,
            "composite_score": self.composite_score,
            "page_count": self.page_count,
            "duration_ms": self.duration_ms,
            "warnings": list(self.warnings),
        }
        return json.dumps(payload, indent=2, sort_keys=True)


def _empty_visual_score() -> VisualScore:
    return VisualScore(
        overall_ssim=1.0,
        min_page_ssim=1.0,
        max_page_ssim=1.0,
        total_diff_pixels=0,
        avg_diff_percentage=0.0,
        page_scores=[],
        worst_page_index=0,
    )


def _run_visual_comparison(
    pdf_pages: list[PilImage],
    reference_dir: Path | None,
    diff_output_dir: Path | None,
    warnings: list[str],
) -> VisualScore:
    if reference_dir is None or not reference_dir.exists():
        warnings.append("No reference images — visual comparison skipped.")
        return _empty_visual_score()
    try:
        ref_images = load_reference_images(reference_dir)
    except FileNotFoundError as exc:
        warnings.append(f"reference dir invalid: {exc}")
        return _empty_visual_score()

    comparisons = []
    for index, (ref, pdf_img) in enumerate(zip(ref_images, pdf_pages, strict=False)):
        comp = compare_page(ref, pdf_img)
        comparisons.append(comp)
        if diff_output_dir is not None and comp.diff_image is not None:
            diff_output_dir.mkdir(parents=True, exist_ok=True)
            comp.diff_image.save(str(diff_output_dir / f"diff_page_{index + 1:03d}.png"))

    if not comparisons:
        warnings.append("no page comparisons could be performed")
        return _empty_visual_score()

    ssim_scores = [c.ssim_score for c in comparisons]
    diff_pcts = [c.diff_pixel_percentage for c in comparisons]
    return VisualScore(
        overall_ssim=sum(ssim_scores) / len(ssim_scores),
        min_page_ssim=min(ssim_scores),
        max_page_ssim=max(ssim_scores),
        total_diff_pixels=sum(c.diff_pixel_count for c in comparisons),
        avg_diff_percentage=sum(diff_pcts) / len(diff_pcts),
        page_scores=comparisons,
        worst_page_index=ssim_scores.index(min(ssim_scores)),
    )


def _empty_text_result() -> TextValidationResult:
    return TextValidationResult(overall_match_ratio=1.0, total_afp_chars=0, total_matching_chars=0)


def run_qa(
    document_id: str,
    pdf_path: Path,
    reference_dir: Path | None = None,
    afp_text_pages: list[str] | None = None,
    afp_page_count: int | None = None,
    afp_tle: dict[str, str] | None = None,
    weights: ScoringWeights | None = None,
    thresholds: ScoringThresholds | None = None,
    dpi: int = DEFAULT_DPI,
    diff_output_dir: Path | None = None,
) -> QaPipelineResult:
    """Run the full QA pipeline for a single document."""
    start = time.monotonic()
    warnings: list[str] = []

    pdf_pages = rasterize_pdf_to_grayscale(pdf_path, dpi=dpi)
    pdf_page_count = len(pdf_pages)

    visual_score = _run_visual_comparison(pdf_pages, reference_dir, diff_output_dir, warnings)

    page_validation = validate_page_count(afp_page_count or pdf_page_count, pdf_page_count)
    if not page_validation.pages_match:
        warnings.append(page_validation.message)

    if afp_text_pages:
        text_validation = compare_text(afp_text_pages, extract_pdf_text(pdf_path))
    else:
        warnings.append("No AFP text provided — text validation skipped.")
        text_validation = _empty_text_result()

    tle_validation: TleValidationResult | None = None
    if afp_tle:
        tle_validation = validate_tle(afp_tle, pdf_path)
        if not tle_validation.passed:
            warnings.append(
                f"TLE validation: {len(tle_validation.missing_tle)} missing key(s)"
            )

    qa_score = compute_qa_score(
        visual_score, text_validation, page_validation, tle_validation,
        weights=weights, thresholds=thresholds,
    )
    decision = route_document(document_id, pdf_path, qa_score)
    duration_ms = int((time.monotonic() - start) * 1000)

    logger.info(
        "qa_result document_id=%s verdict=%s score=%.4f duration_ms=%d warnings=%d",
        document_id,
        decision.verdict.value,
        qa_score.composite_score,
        duration_ms,
        len(warnings),
    )

    return QaPipelineResult(
        document_id=document_id,
        decision=decision,
        visual_score=qa_score.visual_score,
        structural_score=qa_score.structural_score,
        metadata_score=qa_score.metadata_score,
        composite_score=qa_score.composite_score,
        page_count=pdf_page_count,
        duration_ms=duration_ms,
        warnings=warnings,
    )


# Re-exported for convenience in tests.
__all__ = ["QaPipelineResult", "QaVerdict", "run_qa", "datetime"]
