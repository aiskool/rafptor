from __future__ import annotations

from rafptor_collector.generator import afp_constants as c
from rafptor_collector.generator.afp_resources import generate_overlay, generate_standard_font


def test_font_contains_font_descriptor() -> None:
    font = generate_standard_font("C0H20000")
    assert c.SF_FND in font
    assert c.SF_FNC in font
    assert c.SF_FNI in font
    assert c.SF_FNP in font


def test_overlay_contains_begin_and_end_page() -> None:
    overlay = generate_overlay("HEADER01")
    assert c.SF_BPG in overlay
    assert c.SF_EPG in overlay


def test_overlay_footer_text_differs_from_header() -> None:
    header = generate_overlay("HEADER01")
    footer = generate_overlay("FOOTER01")
    assert header != footer
