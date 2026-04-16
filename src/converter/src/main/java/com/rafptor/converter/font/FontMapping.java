package com.rafptor.converter.font;

public record FontMapping(
        String afpCodePage,
        String afpCharsetPrefix,
        String description,
        String ebcdicEncoding,
        String trueTypeFont,
        String fallbackFont,
        double scaleFactor,
        double baselineOffset,
        double defaultPointSize,
        FontMetrics metrics) {

    public FontMapping {
        if (trueTypeFont == null || trueTypeFont.isBlank()) {
            throw new IllegalArgumentException("trueTypeFont must not be blank");
        }
        if (ebcdicEncoding == null || ebcdicEncoding.isBlank()) {
            throw new IllegalArgumentException("ebcdicEncoding must not be blank");
        }
        if (scaleFactor <= 0) {
            throw new IllegalArgumentException("scaleFactor must be > 0");
        }
        if (defaultPointSize <= 0) {
            throw new IllegalArgumentException("defaultPointSize must be > 0");
        }
    }
}
