"""SSIM-based glyph comparison."""

from __future__ import annotations

import numpy as np
from PIL import Image


def ssim(img_a: Image.Image, img_b: Image.Image) -> float:
    a = np.asarray(img_a.convert("L"), dtype=np.float64)
    b = np.asarray(img_b.convert("L"), dtype=np.float64)
    if a.shape != b.shape:
        return 0.0
    if a.size == 0:
        return 1.0
    l_max = 255.0
    c1 = (0.01 * l_max) ** 2
    c2 = (0.03 * l_max) ** 2
    mu_a = float(a.mean())
    mu_b = float(b.mean())
    var_a = float(a.var())
    var_b = float(b.var())
    cov_ab = float(((a - mu_a) * (b - mu_b)).mean())
    num = (2.0 * mu_a * mu_b + c1) * (2.0 * cov_ab + c2)
    den = (mu_a ** 2 + mu_b ** 2 + c1) * (var_a + var_b + c2)
    if den == 0:
        return 1.0
    return float(np.clip(num / den, 0.0, 1.0))


def compare_glyphs(afp_glyph: Image.Image, ttf_glyph: Image.Image) -> float:
    return ssim(afp_glyph, ttf_glyph)


def compare_charsets(
    afp_glyphs: dict[str, Image.Image],
    ttf_glyphs: dict[str, Image.Image],
) -> float:
    common = set(afp_glyphs) & set(ttf_glyphs)
    if not common:
        return 0.0
    scores = [compare_glyphs(afp_glyphs[c], ttf_glyphs[c]) for c in common]
    return sum(scores) / len(scores)
