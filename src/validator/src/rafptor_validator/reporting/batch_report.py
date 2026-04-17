"""Aggregate batch statistics."""

from __future__ import annotations

import json
from collections import Counter
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from ..pipeline.qa_pipeline import QaPipelineResult


def aggregate(results: list["QaPipelineResult"]) -> dict[str, object]:
    if not results:
        return {
            "documents": 0,
            "verdicts": {},
            "avg_composite_score": 0.0,
            "avg_visual_score": 0.0,
            "avg_structural_score": 0.0,
            "avg_metadata_score": 0.0,
            "total_pages": 0,
            "total_duration_ms": 0,
        }
    n = len(results)
    verdicts = Counter(r.decision.verdict.value for r in results)
    return {
        "documents": n,
        "verdicts": dict(verdicts),
        "avg_composite_score": sum(r.composite_score for r in results) / n,
        "avg_visual_score": sum(r.visual_score for r in results) / n,
        "avg_structural_score": sum(r.structural_score for r in results) / n,
        "avg_metadata_score": sum(r.metadata_score for r in results) / n,
        "total_pages": sum(r.page_count for r in results),
        "total_duration_ms": sum(r.duration_ms for r in results),
    }


def render_json(results: list["QaPipelineResult"]) -> str:
    return json.dumps(aggregate(results), indent=2, sort_keys=True)
