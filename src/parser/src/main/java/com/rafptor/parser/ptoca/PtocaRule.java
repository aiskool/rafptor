package com.rafptor.parser.ptoca;

/**
 * One horizontal or vertical filled rule emitted by a PTOCA Draw-I-Axis-Rule
 * (DIR, 0xE4) or Draw-B-Axis-Rule (DBR, 0xE6) control sequence.
 *
 * <p>Coordinates are in PTOCA L-units at the current presentation-text
 * resolution (typically 1440/inch). Thickness is in the same L-units.
 * The direction is carried explicitly so downstream can translate to
 * an {@code IrGraphic} rectangle with the right width/height assignment.
 */
public record PtocaRule(int baselinePosition, int inlinePosition,
                        int lengthLUnits, int thicknessLUnits,
                        Direction direction, String colorHex) {

    public enum Direction {
        /** Horizontal rule — extends along the I-axis (inline). */
        I_AXIS,
        /** Vertical rule — extends along the B-axis (baseline). */
        B_AXIS
    }

    public PtocaRule {
        if (lengthLUnits < 0 || thicknessLUnits < 0) {
            throw new IllegalArgumentException("length and thickness must not be negative");
        }
        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }
        if (colorHex == null || colorHex.isBlank()) {
            colorHex = "#000000";
        }
    }
}
