"""Per-document JSON report."""

from __future__ import annotations

import json
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from ..pipeline.qa_pipeline import QaPipelineResult


def render_json(result: "QaPipelineResult") -> str:
    return result.to_json()


def render_summary(result: "QaPipelineResult") -> dict[str, object]:
    return {
        "document_id": result.document_id,
        "verdict": result.decision.verdict.value,
        "composite_score": round(result.composite_score, 4),
        "visual_score": round(result.visual_score, 4),
        "structural_score": round(result.structural_score, 4),
        "metadata_score": round(result.metadata_score, 4),
        "page_count": result.page_count,
        "duration_ms": result.duration_ms,
        "warnings": list(result.warnings),
    }


def render_summary_json(result: "QaPipelineResult") -> str:
    return json.dumps(render_summary(result), indent=2, sort_keys=True)
