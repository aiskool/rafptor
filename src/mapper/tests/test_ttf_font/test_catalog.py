from __future__ import annotations

from pathlib import Path

from rafptor_mapper.ttf_font.catalog import FontCatalog, classify_family, classify_style


def test_classify_family_monospace() -> None:
    assert classify_family("LiberationMono-Regular") == "monospace"
    assert classify_family("Courier-New") == "monospace"


def test_classify_family_serif() -> None:
    assert classify_family("LiberationSerif") == "serif"
    assert classify_family("Garamond") == "serif"


def test_classify_family_sans() -> None:
    assert classify_family("Arial") == "sans-serif"
    assert classify_family("Helvetica-Bold") == "sans-serif"


def test_classify_style_bold() -> None:
    assert classify_style("Helvetica-Bold") == "bold"
    assert classify_style("LiberationSansBoldItalic") == "bold-italic"


def test_empty_catalog(tmp_path: Path) -> None:
    catalog = FontCatalog(tmp_path)
    assert catalog.get_all() == []
