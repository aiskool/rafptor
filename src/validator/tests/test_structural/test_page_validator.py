from __future__ import annotations

from rafptor_validator.structural.page_validator import validate_page_count


def test_pages_match() -> None:
    result = validate_page_count(5, 5)
    assert result.pages_match is True
    assert "OK" in result.message


def test_pages_mismatch() -> None:
    result = validate_page_count(5, 3)
    assert result.pages_match is False
    assert "MISMATCH" in result.message
