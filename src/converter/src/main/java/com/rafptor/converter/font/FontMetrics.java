package com.rafptor.converter.font;

public record FontMetrics(double avgCharWidthPt, double ascentPt, double descentPt) {

    public FontMetrics {
        if (avgCharWidthPt < 0 || ascentPt < 0 || descentPt < 0) {
            throw new IllegalArgumentException("metrics must be non-negative");
        }
    }

    public static FontMetrics defaults() {
        return new FontMetrics(6.0, 8.0, 2.0);
    }
}
