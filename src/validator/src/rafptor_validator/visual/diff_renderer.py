"""Visual helpers to produce review-ready PNGs.

The low-level per-page diff image is already produced by
:func:`compare_page`; this module currently provides a thin wrapper that
overlays a low-SSIM-zone heatmap on a base image. Reporting features
evolve in a later phase.
"""

from __future__ import annotations

from PIL import Image, ImageDraw

from ..config import SSIM_GRID_COLS, SSIM_GRID_ROWS


def draw_ssim_heatmap(
    base: Image.Image,
    ssim_map: list[list[float]],
    threshold: float = 0.85,
) -> Image.Image:
    """Annotate ``base`` with red rectangles where per-zone SSIM < threshold."""
    if not ssim_map:
        return base.copy()
    rows = len(ssim_map)
    cols = len(ssim_map[0]) if ssim_map[0] else SSIM_GRID_COLS
    if rows <= 0 or cols <= 0:
        return base.copy()
    overlay = base.convert("RGB").copy()
    draw = ImageDraw.Draw(overlay)
    w, h = overlay.size
    cell_w = max(1, w // cols)
    cell_h = max(1, h // rows)
    for row_idx, row in enumerate(ssim_map):
        for col_idx, score in enumerate(row):
            if score >= threshold:
                continue
            x1 = col_idx * cell_w
            y1 = row_idx * cell_h
            x2 = (col_idx + 1) * cell_w if col_idx < cols - 1 else w
            y2 = (row_idx + 1) * cell_h if row_idx < rows - 1 else h
            draw.rectangle((x1, y1, x2, y2), outline=(255, 50, 50), width=3)
    _ = SSIM_GRID_ROWS  # silence "unused import" lint when grid size is fixed elsewhere
    return overlay
