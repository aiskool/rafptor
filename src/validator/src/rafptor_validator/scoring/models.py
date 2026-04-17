"""Scoring and routing dataclasses."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Any


class QaVerdict(str, Enum):
    ACCEPTED = "accepted"
    NEEDS_REVIEW = "needs_review"
    REJECTED = "rejected"


@dataclass
class QaScore:
    visual_score: float
    structural_score: float
    metadata_score: float
    composite_score: float
    verdict: QaVerdict
    details: dict[str, Any] = field(default_factory=dict)


@dataclass
class QaDecision:
    document_id: str
    verdict: QaVerdict
    composite_score: float
    pdf_path: Path
    timestamp: datetime
    action: str
    reason: str
