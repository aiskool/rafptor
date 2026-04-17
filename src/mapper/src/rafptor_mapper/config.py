"""Global configuration constants."""

from __future__ import annotations

MAX_CHARSET_SIZE = 10 * 1024 * 1024   # 10 MB
GLYPH_TARGET_SIZE = 64                # normalised glyph canvas
DEFAULT_AFP_RESOLUTION = 240          # dpi
DEFAULT_TTF_RENDER_SIZE = 48          # px at render time

CONFIDENCE_HIGH = 0.85
CONFIDENCE_MEDIUM = 0.70

DEFAULT_WEIGHTS = {"visual": 0.4, "metrics": 0.4, "coverage": 0.2}
