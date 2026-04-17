from __future__ import annotations

from pathlib import Path

from rafptor_validator.scoring.models import QaScore, QaVerdict
from rafptor_validator.scoring.router import route_document


def _score(verdict: QaVerdict, composite: float) -> QaScore:
    return QaScore(
        visual_score=composite,
        structural_score=composite,
        metadata_score=composite,
        composite_score=composite,
        verdict=verdict,
        details={"ssim_min_page": composite, "text_match_ratio": composite, "pages_match": True},
    )


def test_accepted_maps_to_archive() -> None:
    pdf = Path("/tmp/doc1.pdf")  # noqa: S108
    decision = route_document("doc1", pdf, _score(QaVerdict.ACCEPTED, 0.95))
    assert decision.action == "archive"
    assert "auto-validated" in decision.reason.lower()


def test_rejected_maps_to_reconversion() -> None:
    pdf = Path("/tmp/doc1.pdf")  # noqa: S108
    decision = route_document("doc1", pdf, _score(QaVerdict.REJECTED, 0.5))
    assert decision.action == "flag_for_reconversion"
