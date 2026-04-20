package com.rafptor.parser.ptoca;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Phase7ColorTest {

    @Test
    void cmyk_black_is_000() {
        assertEquals("#000000", PtocaParser.cmykToRgb(0, 0, 0, 255));
    }

    @Test
    void cmyk_white_is_fff() {
        assertEquals("#FFFFFF", PtocaParser.cmykToRgb(0, 0, 0, 0));
    }

    @Test
    void cmyk_pure_cyan_approximates_sRGB() {
        // 100% cyan, no black → full green+blue, no red
        assertEquals("#00FFFF", PtocaParser.cmykToRgb(255, 0, 0, 0));
    }

    @Test
    void highlight_palette_indexes() {
        assertEquals("#000000", PtocaParser.highlightToRgb(0));
        assertEquals("#2196F3", PtocaParser.highlightToRgb(1));
        assertEquals("#E53935", PtocaParser.highlightToRgb(2));
        assertEquals("#FFFFFF", PtocaParser.highlightToRgb(7));
        assertEquals("#000000", PtocaParser.highlightToRgb(999));
    }

    @Test
    void cielab_pure_black_round_trip() {
        String rgb = PtocaParser.cielabToRgb(0, 0, 0);
        assertEquals("#000000", rgb);
    }
}
