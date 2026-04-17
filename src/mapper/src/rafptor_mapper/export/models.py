"""Export dataclasses."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class MappingEntry:
    afp_codepage: str
    afp_charset_prefix: str
    description: str
    ebcdic_encoding: str
    truetype_font: str
    fallback_font: str = "Helvetica"
    scale_factor: float = 1.0
    baseline_offset: float = 0.0
    default_point_size: float = 10.0
    confidence: str = "medium"

    def to_dict(self) -> dict[str, object]:
        return {
            "afp_codepage": self.afp_codepage,
            "afp_charset_prefix": self.afp_charset_prefix,
            "description": self.description,
            "ebcdic_encoding": self.ebcdic_encoding,
            "truetype_font": self.truetype_font,
            "fallback_font": self.fallback_font,
            "scale_factor": self.scale_factor,
            "baseline_offset": self.baseline_offset,
            "default_point_size": self.default_point_size,
            "confidence": self.confidence,
        }


@dataclass
class MappingReport:
    accepted: list[MappingEntry] = field(default_factory=list)
    needs_review: list[MappingEntry] = field(default_factory=list)
    total_candidates: int = 0
