from __future__ import annotations

from PIL import Image

from rafptor_validator.visual.comparator import (
    compare_page,
    compute_pixel_diff,
    compute_ssim,
    compute_ssim_map,
)


def test_ssim_identical_is_one(reference_image: Image.Image) -> None:
    assert compute_ssim(reference_image, reference_image) == 1.0


def test_ssim_similar_is_high(
    reference_image: Image.Image, test_image_similar: Image.Image
) -> None:
    score = compute_ssim(reference_image, test_image_similar)
    assert 0.9 <= score < 1.0


def test_ssim_different_is_not_one(
    reference_image: Image.Image, test_image_different: Image.Image
) -> None:
    score = compute_ssim(reference_image, test_image_different)
    assert score < 1.0


def test_ssim_map_shape_matches_grid(reference_image: Image.Image) -> None:
    grid = compute_ssim_map(reference_image, reference_image, grid_rows=4, grid_cols=3)
    assert len(grid) == 4
    assert all(len(row) == 3 for row in grid)
    assert all(cell == 1.0 for row in grid for cell in row)


def test_pixel_diff_identical_is_zero(reference_image: Image.Image) -> None:
    count, percentage, diff_img = compute_pixel_diff(reference_image, reference_image)
    assert count == 0
    assert percentage == 0.0
    assert diff_img.size == reference_image.size


def test_pixel_diff_flags_differences(
    reference_image: Image.Image, test_image_different: Image.Image
) -> None:
    count, percentage, _ = compute_pixel_diff(
        reference_image, test_image_different, tolerance=0
    )
    assert count > 0
    assert percentage > 0.0


def test_compare_page_returns_full_comparison(reference_image: Image.Image) -> None:
    comp = compare_page(reference_image, reference_image)
    assert comp.ssim_score == 1.0
    assert comp.diff_pixel_count == 0
    assert comp.diff_image is not None
