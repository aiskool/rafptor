"""Minimal HTML report (dependency-free)."""

from __future__ import annotations

import html
import json
from typing import Any


def render_html(report: dict[str, Any]) -> str:
    title = "Rafptor font mapping report"
    payload = html.escape(json.dumps(report, indent=2, sort_keys=True))
    return (
        "<!DOCTYPE html>\n"
        "<html lang='en'><head><meta charset='utf-8'>"
        f"<title>{html.escape(title)}</title></head>"
        "<body style='font-family: system-ui, sans-serif; max-width: 900px; margin: 40px auto;'>"
        f"<h1>{html.escape(title)}</h1>"
        f"<pre style='background:#f5f5f5; padding:16px; border-radius:4px;'>{payload}</pre>"
        "</body></html>"
    )
