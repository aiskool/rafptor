from __future__ import annotations

from rafptor_validator.structural.text_validator import compare_text


def test_identical_text_ratio_is_one() -> None:
    result = compare_text(["Hello world"], ["Hello world"])
    assert result.overall_match_ratio == 1.0


def test_empty_afp_returns_one() -> None:
    result = compare_text([""], [""])
    assert result.overall_match_ratio == 1.0


def test_extra_pdf_text_does_not_raise() -> None:
    result = compare_text(["Short"], ["Short plus extra"])
    assert result.overall_match_ratio == 1.0
    assert result.page_results[0]["pdf_chars"] > result.page_results[0]["afp_chars"]
