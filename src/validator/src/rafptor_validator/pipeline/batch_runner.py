"""Sequential batch runner. Parallelism is a follow-up ticket."""

from __future__ import annotations

import logging
from pathlib import Path

from ..config import DEFAULT_DPI
from ..scoring.thresholds import ScoringThresholds, ScoringWeights
from .qa_pipeline import QaPipelineResult, run_qa

logger = logging.getLogger(__name__)


def run_batch(
    pdf_dir: Path,
    reference_dir: Path | None = None,
    thresholds: ScoringThresholds | None = None,
    weights: ScoringWeights | None = None,
    dpi: int = DEFAULT_DPI,
) -> list[QaPipelineResult]:
    pdfs = sorted(p for p in pdf_dir.glob("*.pdf"))
    if not pdfs:
        logger.warning("no PDF files in %s", pdf_dir)
        return []
    results: list[QaPipelineResult] = []
    for pdf in pdfs:
        result = run_qa(
            document_id=pdf.stem,
            pdf_path=pdf,
            reference_dir=reference_dir,
            thresholds=thresholds,
            weights=weights,
            dpi=dpi,
        )
        results.append(result)
    return results
