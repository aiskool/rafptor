from __future__ import annotations

from PIL import Image

from rafptor_mapper.matching.pixel_matcher import compare_charsets, ssim


def test_identical_ssim_is_one(letter_image: Image.Image) -> None:
    assert ssim(letter_image, letter_image) == 1.0


def test_different_ssim_is_below_one(
    letter_image: Image.Image, blank_image: Image.Image
) -> None:
    assert ssim(letter_image, blank_image) < 1.0


def test_shape_mismatch_returns_zero() -> None:
    a = Image.new("L", (32, 32), 255)
    b = Image.new("L", (64, 64), 255)
    assert ssim(a, b) == 0.0


def test_charset_compare_empty_returns_zero() -> None:
    assert compare_charsets({}, {}) == 0.0
