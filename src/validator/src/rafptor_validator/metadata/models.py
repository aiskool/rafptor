"""Metadata-validation dataclasses."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class TleValidationResult:
    total_tle: int
    preserved_tle: int
    missing_tle: list[str] = field(default_factory=list)
    match_ratio: float = 0.0
    passed: bool = False


@dataclass
class PdfAValidationResult:
    has_output_intent: bool
    conformance_declared: str | None
    passed: bool
