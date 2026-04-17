"""Minimal HTML renderer.

Full Jinja2-driven reports are deferred. This module produces a self-
contained HTML page from a JSON aggregate so the dashboard/reviewer flow
has a tangible artefact to link to.
"""

from __future__ import annotations

import html
import json
from typing import Any


def render_batch_html(aggregate: dict[str, Any]) -> str:
    title = html.escape("Rafptor QA — Batch report")
    payload = html.escape(json.dumps(aggregate, indent=2, sort_keys=True))
    return (
        "<!DOCTYPE html>\n"
        "<html lang='en'><head><meta charset='utf-8'>"
        f"<title>{title}</title></head>"
        "<body style='font-family: system-ui, sans-serif; max-width: 900px; margin: 40px auto;'>"
        f"<h1>{title}</h1>"
        f"<pre style='background:#f5f5f5; padding:16px; border-radius:4px;'>{payload}</pre>"
        "</body></html>"
    )
