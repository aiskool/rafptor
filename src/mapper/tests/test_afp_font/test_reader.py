from __future__ import annotations

from pathlib import Path

import pytest

from rafptor_mapper.afp_font.reader import AfpFontReader


def test_reads_unknown_fields_tolerantly(tiny_afp_charset: Path) -> None:
    font = AfpFontReader().read(tiny_afp_charset)
    assert font.info.glyph_count == 0
    assert font.info.metadata.resolution_x > 0


def test_rejects_oversized_file(tmp_path: Path) -> None:
    path = tmp_path / "big.bin"
    path.write_bytes(b"\x00" * 10)
    reader = AfpFontReader(max_size=5)
    with pytest.raises(ValueError):
        reader.read(path)


def test_rejects_missing_carriage_control(tmp_path: Path) -> None:
    path = tmp_path / "bad.bin"
    path.write_bytes(b"\x00\x00\x09\xD3\xFF\xFF\x00\x00\x00\x00")
    with pytest.raises(ValueError):
        AfpFontReader().read(path)
