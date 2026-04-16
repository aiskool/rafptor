package com.rafptor.converter.render;

import java.awt.Color;

/** Parses the "#RRGGBB" color strings stored in the IR. */
final class ColorUtil {

    private ColorUtil() {
    }

    static Color parse(String hex) {
        if (hex == null || hex.isEmpty()) {
            return Color.BLACK;
        }
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        if (s.length() != 6) {
            return Color.BLACK;
        }
        try {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            return new Color(r, g, b);
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }
}
