"""EBCDIC → Unicode mapping using bundled JSON tables."""

from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path

_DATA_DIR = Path(__file__).resolve().parents[3] / "data" / "ibm_codepages"


class CodepageUnavailableError(Exception):
    pass


@lru_cache(maxsize=8)
def load_codepage(name: str) -> dict[int, str]:
    """Return a ``byte → unicode`` mapping for the given code-page name (e.g. ``"cp500"``)."""
    canonical = name.lower()
    path = _DATA_DIR / f"{canonical}.json"
    if not path.exists():
        raise CodepageUnavailableError(
            f"code page {name!r} is not bundled (looked for {path})"
        )
    data = json.loads(path.read_text())
    raw_mapping = data.get("mapping", {})
    result: dict[int, str] = {}
    for key, value in raw_mapping.items():
        try:
            byte = int(key, 16)
        except ValueError:
            continue
        if value is not None:
            result[byte] = value
    return result


def codepoint_to_unicode(byte: int, codepage: str) -> str | None:
    if not 0 <= byte <= 255:
        raise ValueError("byte must be in [0, 255]")
    return load_codepage(codepage).get(byte)
