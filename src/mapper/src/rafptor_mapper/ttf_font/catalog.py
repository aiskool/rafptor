"""Scan a directory of ``.ttf`` files and classify them by family."""

from __future__ import annotations

from pathlib import Path

from .models import TtfFontInfo


def classify_family(name: str) -> str:
    lower = name.lower()
    if any(token in lower for token in ("mono", "courier", "consolas", "typewriter")):
        return "monospace"
    if any(token in lower for token in ("serif", "times", "roman", "garamond")):
        return "serif"
    if any(token in lower for token in ("sans", "arial", "helvetica", "verdana")):
        return "sans-serif"
    return "unknown"


def classify_style(name: str) -> str:
    lower = name.lower()
    if "bolditalic" in lower.replace(" ", "") or ("bold" in lower and "italic" in lower):
        return "bold-italic"
    if "italic" in lower or "oblique" in lower:
        return "italic"
    if "bold" in lower:
        return "bold"
    return "regular"


class FontCatalog:
    """Directory-scanner catalogue with no external TTF parsing beyond filename heuristics."""

    def __init__(self, fonts_dir: Path) -> None:
        self.fonts_dir = fonts_dir
        self._fonts: list[TtfFontInfo] = []
        self._load()

    def _load(self) -> None:
        if not self.fonts_dir.exists():
            return
        for ttf in sorted(self.fonts_dir.rglob("*.ttf")):
            stem = ttf.stem
            self._fonts.append(
                TtfFontInfo(
                    name=stem,
                    family=classify_family(stem),
                    path=ttf,
                    style=classify_style(stem),
                )
            )

    def get_all(self) -> list[TtfFontInfo]:
        return list(self._fonts)

    def get_monospace(self) -> list[TtfFontInfo]:
        return [f for f in self._fonts if f.family == "monospace"]

    def get_serif(self) -> list[TtfFontInfo]:
        return [f for f in self._fonts if f.family == "serif"]

    def get_sans(self) -> list[TtfFontInfo]:
        return [f for f in self._fonts if f.family == "sans-serif"]
