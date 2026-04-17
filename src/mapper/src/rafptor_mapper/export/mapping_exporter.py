"""Produce a JSON file compatible with the converter's StandardFontMapper."""

from __future__ import annotations

import json
from pathlib import Path

from ..matching.models import MatchResult
from .models import MappingEntry

_CONFIDENCE_ORDER = {"high": 2, "medium": 1, "low": 0}


def _to_entry(result: MatchResult) -> MappingEntry:
    return MappingEntry(
        afp_codepage="",
        afp_charset_prefix=result.afp_font_name,
        description=f"AI-matched: {result.afp_font_name} → {result.ttf_font_name}",
        ebcdic_encoding="IBM500",
        truetype_font=result.ttf_font_name,
        confidence=result.confidence,
    )


def export_mapping(
    results: list[MatchResult],
    output_path: Path,
    min_confidence: str = "medium",
) -> dict[str, object]:
    level = _CONFIDENCE_ORDER.get(min_confidence, 1)
    accepted: list[MappingEntry] = []
    needs_review: list[MappingEntry] = []
    for r in results:
        entry = _to_entry(r)
        if _CONFIDENCE_ORDER.get(r.confidence, 0) >= level:
            accepted.append(entry)
        else:
            needs_review.append(entry)

    payload = {
        "version": "1.0",
        "source": "rafptor-mapper-ai",
        "description": "AI-generated font mappings — compatible with StandardFontMapper.java",
        "mappings": [entry.to_dict() for entry in accepted],
    }
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, indent=2))

    review_path = output_path.with_suffix(".review.json")
    review_payload = {
        "version": "1.0",
        "source": "rafptor-mapper-ai",
        "needs_review": [entry.to_dict() for entry in needs_review],
    }
    review_path.write_text(json.dumps(review_payload, indent=2))

    return {
        "accepted": len(accepted),
        "needs_review": len(needs_review),
        "output": str(output_path),
        "review_file": str(review_path),
    }
