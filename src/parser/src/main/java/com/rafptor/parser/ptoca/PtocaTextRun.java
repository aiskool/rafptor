package com.rafptor.parser.ptoca;

/**
 * One decoded run of text extracted from a PTOCA stream.
 *
 * <p>Coordinates are in PTOCA L-units (1/1440 inch by default). The conversion to
 * millimetres / points is the responsibility of the renderer (Module 5).
 */
public record PtocaTextRun(int localFontId, int baselinePosition, int inlinePosition, String text) {

    public PtocaTextRun {
        if ((localFontId & ~0xFF) != 0) {
            throw new IllegalArgumentException("localFontId must be in [0,255]");
        }
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
    }
}
