"""Position-tolerance checks (stub for Phase 3)."""

from __future__ import annotations


def within_tolerance(
    ref_x: float,
    ref_y: float,
    test_x: float,
    test_y: float,
    tolerance_pt: float = 2.0,
) -> bool:
    """True if ``(test_x, test_y)`` is within ``tolerance_pt`` of ``(ref_x, ref_y)``."""
    if tolerance_pt < 0:
        raise ValueError("tolerance_pt must be >= 0")
    return abs(ref_x - test_x) <= tolerance_pt and abs(ref_y - test_y) <= tolerance_pt
