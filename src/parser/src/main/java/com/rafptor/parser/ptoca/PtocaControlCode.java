package com.rafptor.parser.ptoca;

/**
 * PTOCA control-sequence opcode byte values. Only the subset that Rafptor
 * currently handles is enumerated here; unknown codes are skipped by
 * {@link PtocaParser} using the declared length byte.
 */
public final class PtocaControlCode {

    // Canonical MO:DCA PTOCA opcodes with LSB cleared. On the wire the LSB
    // can be set when a chained continuation follows; {@link PtocaParser}
    // always masks with 0xFE before comparing against these constants.
    public static final int SET_CODED_FONT_LOCAL = 0xF0;
    public static final int ABSOLUTE_MOVE_BASELINE = 0xD2;
    public static final int ABSOLUTE_MOVE_INLINE = 0xC6;
    public static final int RELATIVE_MOVE_INLINE = 0xC8;
    public static final int TRANSPARENT_DATA = 0xDA;
    public static final int DRAW_I_AXIS_RULE = 0xE6;
    public static final int DRAW_B_AXIS_RULE = 0xE4;

    private PtocaControlCode() {
    }
}
