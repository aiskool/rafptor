"""AFP font dataclasses."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class FontMetadata:
    family_name: str = ""
    resolution_x: int = 240
    resolution_y: int = 240
    max_char_width: int = 0
    max_char_height: int = 0
    default_baseline: int = 0
    orientation: int = 0


@dataclass
class GlyphEntry:
    gcgid: str
    width: int
    height: int
    baseline_offset: int
    bitmap: bytes


@dataclass
class CharsetInfo:
    name: str
    metadata: FontMetadata
    glyph_count: int
    warnings: list[str] = field(default_factory=list)


@dataclass
class AfpFont:
    info: CharsetInfo
    glyphs: list[GlyphEntry]


@dataclass
class AfpGlyph:
    gcgid: str
    unicode_char: str | None
    width_units: int
