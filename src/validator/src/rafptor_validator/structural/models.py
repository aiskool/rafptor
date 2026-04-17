"""Structural-validation dataclasses."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class PageValidationResult:
    afp_page_count: int
    pdf_page_count: int
    pages_match: bool
    message: str


@dataclass
class TextValidationResult:
    overall_match_ratio: float
    total_afp_chars: int
    total_matching_chars: int
    page_results: list[dict] = field(default_factory=list)


@dataclass
class ObjectCounts:
    text_blocks: int
    images: int
    paths: int
    annotations: int
