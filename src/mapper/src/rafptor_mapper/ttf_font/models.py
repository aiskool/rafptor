"""TrueType dataclasses."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass
class TtfFontInfo:
    name: str
    family: str  # "monospace" / "serif" / "sans-serif" / "unknown"
    path: Path
    style: str = "regular"
    license: str = "unknown"
    metrics_compatible: str | None = None


@dataclass
class TtfGlyph:
    char: str
    width_px: int
    height_px: int
