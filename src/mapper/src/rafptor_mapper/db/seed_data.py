"""Seed mappings (derived from the converter's standard-mappings.json)."""

from __future__ import annotations

from typing import Any

SEED_MAPPINGS: list[dict[str, Any]] = [
    {
        "afp_codepage": "T1V10500",
        "afp_charset_prefix": "C0H200",
        "description": "IBM Courier, Latin-1",
        "ebcdic_encoding": "IBM500",
        "truetype_font": "Liberation Mono",
        "fallback_font": "Courier",
        "confidence": "high",
    },
    {
        "afp_codepage": "T1V10500",
        "afp_charset_prefix": "C0N200",
        "description": "IBM Sonoran Sans Serif, Latin-1",
        "ebcdic_encoding": "IBM500",
        "truetype_font": "Liberation Sans",
        "fallback_font": "Helvetica",
        "confidence": "high",
    },
    {
        "afp_codepage": "T1V10500",
        "afp_charset_prefix": "C0S200",
        "description": "IBM Sonoran Serif, Latin-1",
        "ebcdic_encoding": "IBM500",
        "truetype_font": "Liberation Serif",
        "fallback_font": "Times-Roman",
        "confidence": "high",
    },
    {
        "afp_codepage": "T1GI1147",
        "afp_charset_prefix": "C0H200",
        "description": "IBM Courier, French EBCDIC (cp1147)",
        "ebcdic_encoding": "IBM1147",
        "truetype_font": "Liberation Mono",
        "fallback_font": "Courier",
        "confidence": "high",
    },
]
