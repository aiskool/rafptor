"""Dataclasses describing visual-comparison outcomes."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from PIL.Image import Image


@dataclass
class PageComparison:
    """Outcome of comparing one reference page against one test page."""

    ssim_score: float
    ssim_map: list[list[float]]
    diff_pixel_count: int
    diff_pixel_percentage: float
    diff_image: "Image | None" = None


@dataclass
class VisualScore:
    """Aggregate visual score across all pages of a document."""

    overall_ssim: float
    min_page_ssim: float
    max_page_ssim: float
    total_diff_pixels: int
    avg_diff_percentage: float
    page_scores: list[PageComparison] = field(default_factory=list)
    worst_page_index: int = 0
