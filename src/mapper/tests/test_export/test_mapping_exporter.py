from __future__ import annotations

import json
from pathlib import Path

from rafptor_mapper.export.mapping_exporter import export_mapping
from rafptor_mapper.matching.models import MatchResult


def _result(name: str, score: float, confidence: str) -> MatchResult:
    return MatchResult(
        afp_font_name=name,
        ttf_font_name="Liberation Mono",
        ttf_font_path="/fake/lib-mono.ttf",
        visual_score=score,
        metrics_score=score,
        combined_score=score,
        confidence=confidence,
        char_coverage=1.0,
    )


def test_accepted_and_review_split(tmp_path: Path) -> None:
    results = [
        _result("C0H200", 0.90, "high"),
        _result("C0N200", 0.55, "low"),
    ]
    out = tmp_path / "mappings.json"
    summary = export_mapping(results, out, min_confidence="medium")
    assert summary["accepted"] == 1
    assert summary["needs_review"] == 1
    payload = json.loads(out.read_text())
    assert payload["source"] == "rafptor-mapper-ai"
    assert len(payload["mappings"]) == 1


def test_converter_compatible_keys(tmp_path: Path) -> None:
    out = tmp_path / "m.json"
    export_mapping([_result("C0H200", 0.95, "high")], out)
    payload = json.loads(out.read_text())
    entry = payload["mappings"][0]
    required = {
        "afp_codepage",
        "afp_charset_prefix",
        "ebcdic_encoding",
        "truetype_font",
        "fallback_font",
        "scale_factor",
        "baseline_offset",
        "default_point_size",
    }
    assert required.issubset(entry)


def test_empty_results_produces_empty_file(tmp_path: Path) -> None:
    out = tmp_path / "m.json"
    summary = export_mapping([], out)
    assert summary["accepted"] == 0
    assert summary["needs_review"] == 0
