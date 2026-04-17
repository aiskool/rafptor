"""Pipeline-level tests that do not require rendering a real PDF.

Rendering a PDF requires PyMuPDF and a non-trivial source; the end-to-end
pipeline test lives as an integration suite run in CI with the parser
and converter. Here we only exercise the JSON serialisation and the
verdict plumbing.
"""

from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path

from rafptor_validator.pipeline.qa_pipeline import QaPipelineResult
from rafptor_validator.scoring.models import QaDecision, QaVerdict


def _result() -> QaPipelineResult:
    decision = QaDecision(
        document_id="doc-1",
        verdict=QaVerdict.ACCEPTED,
        composite_score=0.95,
        pdf_path=Path("/tmp/doc-1.pdf"),
        timestamp=datetime(2026, 4, 17, 12, 0, tzinfo=timezone.utc),
        action="archive",
        reason="ok",
    )
    return QaPipelineResult(
        document_id="doc-1",
        decision=decision,
        visual_score=0.96,
        structural_score=0.94,
        metadata_score=0.95,
        composite_score=0.95,
        page_count=3,
        duration_ms=1234,
        warnings=["no reference images"],
    )


def test_result_serializes_to_stable_json() -> None:
    payload = _result().to_json()
    parsed = json.loads(payload)
    assert parsed["document_id"] == "doc-1"
    assert parsed["decision"]["verdict"] == "accepted"
    assert parsed["composite_score"] == 0.95
    assert parsed["warnings"] == ["no reference images"]


def test_batch_aggregate_counts_verdicts() -> None:
    from rafptor_validator.reporting.batch_report import aggregate

    r = _result()
    summary = aggregate([r, r, r])
    assert summary["documents"] == 3
    assert summary["verdicts"] == {"accepted": 3}
    assert abs(summary["avg_composite_score"] - 0.95) < 1e-9
