from __future__ import annotations

from rafptor_validator.metadata.models import TleValidationResult


def test_result_defaults_are_non_passing() -> None:
    result = TleValidationResult(total_tle=0, preserved_tle=0)
    assert result.match_ratio == 0.0
    assert result.passed is False
    assert result.missing_tle == []
