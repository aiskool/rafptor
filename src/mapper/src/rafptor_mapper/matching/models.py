"""Matching dataclasses."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class FontMetricsComparison:
    avg_width_diff: float
    max_width_diff: float
    height_diff: float
    baseline_diff: float
    monospace_match: bool
    score: float


@dataclass
class MatchResult:
    afp_font_name: str
    ttf_font_name: str
    ttf_font_path: str
    visual_score: float
    metrics_score: float
    combined_score: float
    confidence: str
    char_coverage: float
    warnings: list[str] = field(default_factory=list)
