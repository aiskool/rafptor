"""Configurable weighting and thresholds."""

from __future__ import annotations

from dataclasses import dataclass

from ..config import (
    DEFAULT_ACCEPT_THRESHOLD,
    DEFAULT_REVIEW_THRESHOLD,
    METADATA_WEIGHT,
    STRUCTURAL_WEIGHT,
    VISUAL_WEIGHT,
)


@dataclass
class ScoringWeights:
    visual: float = VISUAL_WEIGHT
    structural: float = STRUCTURAL_WEIGHT
    metadata: float = METADATA_WEIGHT

    def __post_init__(self) -> None:
        total = self.visual + self.structural + self.metadata
        if total <= 0:
            raise ValueError("weights sum must be > 0")


@dataclass
class ScoringThresholds:
    accept: float = DEFAULT_ACCEPT_THRESHOLD
    review: float = DEFAULT_REVIEW_THRESHOLD

    def __post_init__(self) -> None:
        if not 0.0 <= self.review <= self.accept <= 1.0:
            raise ValueError("thresholds must satisfy 0 <= review <= accept <= 1")
