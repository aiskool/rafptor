"""CNN-based feature matching (Phase 2 — not yet implemented).

This module is reserved for a pretrained-embedding glyph matcher
(CLIP / contrastive / custom glyph CNN). Phase 4 of the Module 4 plan
wires a small model; for the current release the project relies
exclusively on SSIM + metric comparison.
"""

from __future__ import annotations

from typing import Any


class NotImplementedFeatureMatcher:
    """Placeholder that raises when called."""

    def compare(self, *_: Any, **__: Any) -> float:
        raise NotImplementedError("CNN feature matching lands in Phase 2")
