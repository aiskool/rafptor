"""SSIM and pixel-diff comparison.

SSIM is implemented from scratch following Wang et al. (2004) with a single
global mean/variance estimate (a simpler alternative to the Gaussian-weighted
window of the original formulation). The formulation is sufficient to give
monotonic, well-behaved scores on document images — identical input yields
exactly ``1.0`` and entirely uncorrelated inputs yield scores close to ``0``.
"""

from __future__ import annotations

import numpy as np
from PIL import Image

from ..config import (
    DEFAULT_PIXEL_TOLERANCE,
    SSIM_GRID_COLS,
    SSIM_GRID_ROWS,
)
from .models import PageComparison


def _ensure_same_size(ref: Image.Image, test: Image.Image) -> Image.Image:
    if ref.size == test.size:
        return test
    return test.resize(ref.size, Image.Resampling.LANCZOS)


def _ssim_single(a: np.ndarray, b: np.ndarray) -> float:
    """SSIM between two already-aligned float64 arrays."""
    if a.size == 0 or b.size == 0:
        return 1.0
    l_max = 255.0
    c1 = (0.01 * l_max) ** 2
    c2 = (0.03 * l_max) ** 2
    mu_a = float(a.mean())
    mu_b = float(b.mean())
    sigma_a_sq = float(a.var())
    sigma_b_sq = float(b.var())
    sigma_ab = float(((a - mu_a) * (b - mu_b)).mean())
    numerator = (2.0 * mu_a * mu_b + c1) * (2.0 * sigma_ab + c2)
    denominator = (mu_a ** 2 + mu_b ** 2 + c1) * (sigma_a_sq + sigma_b_sq + c2)
    if denominator == 0:
        return 1.0
    return float(np.clip(numerator / denominator, 0.0, 1.0))


def compute_ssim(img_ref: Image.Image, img_test: Image.Image) -> float:
    """Return the global SSIM between ``img_ref`` and ``img_test``."""
    ref = img_ref.convert("L")
    test = _ensure_same_size(ref, img_test.convert("L"))
    a = np.asarray(ref, dtype=np.float64)
    b = np.asarray(test, dtype=np.float64)
    return _ssim_single(a, b)


def compute_ssim_map(
    img_ref: Image.Image,
    img_test: Image.Image,
    grid_rows: int = SSIM_GRID_ROWS,
    grid_cols: int = SSIM_GRID_COLS,
) -> list[list[float]]:
    """Return a ``grid_rows × grid_cols`` matrix of local SSIM scores."""
    if grid_rows <= 0 or grid_cols <= 0:
        raise ValueError("grid dimensions must be > 0")
    ref = img_ref.convert("L")
    test = _ensure_same_size(ref, img_test.convert("L"))
    a = np.asarray(ref, dtype=np.float64)
    b = np.asarray(test, dtype=np.float64)
    h, w = a.shape
    cell_h = max(1, h // grid_rows)
    cell_w = max(1, w // grid_cols)
    matrix: list[list[float]] = []
    for row in range(grid_rows):
        row_scores: list[float] = []
        for col in range(grid_cols):
            y1 = row * cell_h
            y2 = (row + 1) * cell_h if row < grid_rows - 1 else h
            x1 = col * cell_w
            x2 = (col + 1) * cell_w if col < grid_cols - 1 else w
            row_scores.append(_ssim_single(a[y1:y2, x1:x2], b[y1:y2, x1:x2]))
        matrix.append(row_scores)
    return matrix


def compute_pixel_diff(
    img_ref: Image.Image,
    img_test: Image.Image,
    tolerance: int = DEFAULT_PIXEL_TOLERANCE,
) -> tuple[int, float, Image.Image]:
    """Return ``(count, percentage, diff_image)`` pixels differing beyond ``tolerance``."""
    if tolerance < 0 or tolerance > 255:
        raise ValueError("tolerance must be in [0, 255]")
    ref = img_ref.convert("L")
    test = _ensure_same_size(ref, img_test.convert("L"))
    a = np.asarray(ref, dtype=np.int16)
    b = np.asarray(test, dtype=np.int16)
    diff = np.abs(a - b)
    mask = diff > tolerance
    count = int(mask.sum())
    total = mask.size or 1
    percentage = count / total
    rgb = np.stack([np.asarray(test)] * 3, axis=-1).astype(np.uint8)
    rgb[mask] = (255, 50, 50)
    return count, percentage, Image.fromarray(rgb, mode="RGB")


def compare_page(
    ref_image: Image.Image,
    test_image: Image.Image,
    tolerance: int = DEFAULT_PIXEL_TOLERANCE,
) -> PageComparison:
    """Full per-page comparison: global SSIM + SSIM map + pixel-diff."""
    ssim_score = compute_ssim(ref_image, test_image)
    ssim_map = compute_ssim_map(ref_image, test_image)
    count, percentage, diff_image = compute_pixel_diff(ref_image, test_image, tolerance)
    return PageComparison(
        ssim_score=ssim_score,
        ssim_map=ssim_map,
        diff_pixel_count=count,
        diff_pixel_percentage=percentage,
        diff_image=diff_image,
    )
