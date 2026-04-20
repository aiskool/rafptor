package com.rafptor.converter.render;

import java.awt.Color;

/**
 * Maps AFP predefined shading pattern IDs (1..16) to a (fillAlpha, isTiling)
 * descriptor the PDF renderer can approximate without PDTilingPattern.
 *
 * <p>For IDs 1..5 a simple alpha-blended fill reproduces the visual density.
 * For IDs 6..10 (line/cross-hatch variants) we fall back to a mid-density
 * solid fill until a proper PDTilingPattern implementation lands.
 */
public final class ShadingPatternResolver {

    public record ShadingSpec(double alpha, boolean tiling, String note) { }

    private ShadingPatternResolver() {
    }

    public static ShadingSpec resolve(int patternId) {
        return switch (patternId) {
            case 1  -> new ShadingSpec(1.00, false, "solid");
            case 2  -> new ShadingSpec(0.75, false, "75% dense");
            case 3  -> new ShadingSpec(0.50, false, "50% medium");
            case 4  -> new ShadingSpec(0.25, false, "25% light");
            case 5  -> new ShadingSpec(0.125, false, "12.5% very light");
            case 6  -> new ShadingSpec(0.35, true, "horizontal hatch (alpha fallback)");
            case 7  -> new ShadingSpec(0.35, true, "vertical hatch (alpha fallback)");
            case 8  -> new ShadingSpec(0.30, true, "diagonal 45° (alpha fallback)");
            case 9  -> new ShadingSpec(0.45, true, "cross hatch (alpha fallback)");
            case 10 -> new ShadingSpec(0.45, true, "diagonal cross (alpha fallback)");
            default -> new ShadingSpec(1.00, false, "unknown → solid");
        };
    }

    /** Blend a base colour with white by the given alpha to approximate a tint. */
    public static Color tint(Color base, double alpha) {
        double a = Math.max(0, Math.min(1, alpha));
        int r = (int) Math.round(base.getRed()   * a + 255 * (1 - a));
        int g = (int) Math.round(base.getGreen() * a + 255 * (1 - a));
        int b = (int) Math.round(base.getBlue()  * a + 255 * (1 - a));
        return new Color(r, g, b);
    }
}
