"""FOCA sub-structure helpers.

Additional parsing helpers live here when the main reader needs richer
semantics. For the current release the core reader extracts everything
needed for matching; this module is reserved for future FOCA fields
(shading patterns, kerning pairs, alternate glyph variants).
"""

from __future__ import annotations

from .models import FontMetadata


def orientation_from_raw(raw: int) -> int:
    """Normalise an AFP orientation value to one of 0/90/180/270 degrees."""
    if raw in (0, 1, 90):
        return 0 if raw == 0 else 90
    if raw in (180, 270):
        return raw
    return 0


def detect_monospace(metadata: FontMetadata, widths: list[int]) -> bool:
    """Return True if the widths distribution matches a monospace font."""
    if not widths:
        return False
    unique = len({w for w in widths if w > 0})
    return unique <= 2
