"""Route a document based on its QA verdict."""

from __future__ import annotations

from datetime import datetime, timezone
from pathlib import Path

from .models import QaDecision, QaScore, QaVerdict

_ACTIONS = {
    QaVerdict.ACCEPTED: "archive",
    QaVerdict.NEEDS_REVIEW: "queue_for_review",
    QaVerdict.REJECTED: "flag_for_reconversion",
}


def _reason(score: QaScore) -> str:
    pct = f"{score.composite_score:.2%}"
    if score.verdict is QaVerdict.ACCEPTED:
        return f"Score {pct} >= accept threshold — auto-validated."
    if score.verdict is QaVerdict.NEEDS_REVIEW:
        worst = score.details.get("ssim_min_page", "n/a")
        return (
            f"Score {pct} between review and accept thresholds — "
            f"human review required (worst-page SSIM={worst})."
        )
    return (
        f"Score {pct} below review threshold — reconversion required "
        f"(text match={score.details.get('text_match_ratio', 'n/a')}, "
        f"pages match={score.details.get('pages_match', 'n/a')})."
    )


def route_document(document_id: str, pdf_path: Path, score: QaScore) -> QaDecision:
    return QaDecision(
        document_id=document_id,
        verdict=score.verdict,
        composite_score=score.composite_score,
        pdf_path=pdf_path,
        timestamp=datetime.now(timezone.utc),
        action=_ACTIONS[score.verdict],
        reason=_reason(score),
    )
