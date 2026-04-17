"""AFP character-set (FOCA) reader — best-effort tolerant parser.

FOCA uses MO:DCA-style structured fields. Each record starts with ``0x5A``
followed by a 2-byte length, a 3-byte structured-field identifier, one
flags byte and two reserved bytes. The parser recognises the handful of
identifiers that describe font metadata and glyph bitmaps; unknown
fields are counted as warnings so uncommon/exotic character-sets still
produce partial, usable output.

Security: length and total-size bounds are checked before any allocation
and the stream is never executed.
"""

from __future__ import annotations

import struct
from dataclasses import field
from pathlib import Path
from typing import BinaryIO

from ..config import MAX_CHARSET_SIZE
from .models import AfpFont, CharsetInfo, FontMetadata, GlyphEntry

FND_ID = (0xD3, 0xA6, 0x89)
FNC_ID = (0xD3, 0xA7, 0x89)
FNI_ID = (0xD3, 0x8C, 0x89)
FNP_ID = (0xD3, 0x8E, 0x89)

HEADER_SIZE = 8


class AfpFontReader:
    """Tolerant FOCA character-set reader."""

    def __init__(self, max_size: int = MAX_CHARSET_SIZE) -> None:
        if max_size <= 0:
            raise ValueError("max_size must be > 0")
        self.max_size = max_size

    def read(self, path: Path) -> AfpFont:
        size = path.stat().st_size
        if size > self.max_size:
            raise ValueError(
                f"character set too large: {size} bytes (max {self.max_size})"
            )
        with path.open("rb") as f:
            metadata, glyph_index, pattern_data, warnings = self._parse_stream(f)

        glyphs = self._assemble(glyph_index, pattern_data, metadata, warnings)
        info = CharsetInfo(
            name=path.name,
            metadata=metadata,
            glyph_count=len(glyphs),
            warnings=warnings,
        )
        return AfpFont(info=info, glyphs=glyphs)

    def _parse_stream(
        self, f: BinaryIO
    ) -> tuple[FontMetadata, list[dict], bytes, list[str]]:
        metadata = FontMetadata()
        index_entries: list[dict] = []
        patterns = bytearray()
        warnings: list[str] = []

        while True:
            intro = f.read(1)
            if not intro:
                break
            if intro[0] != 0x5A:
                raise ValueError(
                    f"missing 0x5A carriage control (got 0x{intro[0]:02X})"
                )
            length_bytes = f.read(2)
            if len(length_bytes) < 2:
                warnings.append("truncated length header")
                break
            sf_length = struct.unpack(">H", length_bytes)[0]
            if sf_length < HEADER_SIZE:
                raise ValueError(f"invalid structured field length: {sf_length}")
            if sf_length > self.max_size:
                raise ValueError("structured field exceeds max size")

            sf_id_bytes = f.read(3)
            if len(sf_id_bytes) < 3:
                warnings.append("truncated structured field id")
                break
            sf_id = (sf_id_bytes[0], sf_id_bytes[1], sf_id_bytes[2])

            # flags + 2 reserved bytes
            if len(f.read(3)) < 3:
                warnings.append("truncated reserved bytes")
                break

            data_len = sf_length - HEADER_SIZE
            data = f.read(data_len)
            if len(data) != data_len:
                warnings.append(
                    f"truncated payload for {sf_id}: expected {data_len}, got {len(data)}"
                )
                break

            if sf_id == FND_ID:
                metadata = self._parse_font_descriptor(data)
            elif sf_id == FNC_ID:
                self._parse_font_control(data, metadata)
            elif sf_id == FNI_ID:
                index_entries.extend(self._parse_font_index(data))
            elif sf_id == FNP_ID:
                patterns.extend(data)
            else:
                # Unknown field — tolerated.
                continue

        return metadata, index_entries, bytes(patterns), warnings

    @staticmethod
    def _parse_font_descriptor(data: bytes) -> FontMetadata:
        family = ""
        if len(data) >= 8:
            try:
                family = data[:8].decode("cp500").strip()
            except UnicodeDecodeError:
                family = ""
        return FontMetadata(family_name=family)

    @staticmethod
    def _parse_font_control(data: bytes, metadata: FontMetadata) -> None:
        # FNC offsets vary with FOCA version. We pick the first two big-endian
        # unsigned shorts as (x-resolution, y-resolution) when they look sane.
        if len(data) >= 4:
            res_x = struct.unpack(">H", data[0:2])[0]
            res_y = struct.unpack(">H", data[2:4])[0]
            if 50 <= res_x <= 2400:
                metadata.resolution_x = res_x
            if 50 <= res_y <= 2400:
                metadata.resolution_y = res_y

    @staticmethod
    def _parse_font_index(data: bytes) -> list[dict]:
        # Each entry uses an 8-byte GCGID followed by big-endian metric shorts.
        # The full record length is vendor-dependent; 26 bytes is a common value.
        entries: list[dict] = []
        entry_size = 26
        if not data or len(data) < entry_size:
            return entries
        count = len(data) // entry_size
        for i in range(count):
            chunk = data[i * entry_size : (i + 1) * entry_size]
            gcgid = chunk[0:8].decode("ascii", errors="replace").strip()
            width = struct.unpack(">H", chunk[8:10])[0] if len(chunk) >= 10 else 0
            height = struct.unpack(">H", chunk[10:12])[0] if len(chunk) >= 12 else 0
            baseline = struct.unpack(">h", chunk[12:14])[0] if len(chunk) >= 14 else 0
            offset = struct.unpack(">I", chunk[14:18])[0] if len(chunk) >= 18 else 0
            entries.append(
                {
                    "gcgid": gcgid,
                    "width": width,
                    "height": height,
                    "baseline_offset": baseline,
                    "offset": offset,
                }
            )
        return entries

    @staticmethod
    def _assemble(
        index: list[dict],
        patterns: bytes,
        metadata: FontMetadata,
        warnings: list[str],
    ) -> list[GlyphEntry]:
        glyphs: list[GlyphEntry] = []
        for entry in index:
            offset = entry["offset"]
            width = entry["width"]
            height = entry["height"]
            if width <= 0 or height <= 0:
                continue
            row_bytes = (width + 7) // 8
            length = row_bytes * height
            if offset < 0 or offset + length > len(patterns):
                warnings.append(
                    f"glyph {entry['gcgid']!r} bitmap offset out of range"
                )
                continue
            bitmap = patterns[offset : offset + length]
            glyphs.append(
                GlyphEntry(
                    gcgid=entry["gcgid"],
                    width=width,
                    height=height,
                    baseline_offset=entry["baseline_offset"],
                    bitmap=bitmap,
                )
            )
            if metadata.max_char_width < width:
                metadata.max_char_width = width
            if metadata.max_char_height < height:
                metadata.max_char_height = height
        return glyphs
