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
    /**
     * PTOCA Draw I-axis Rule (DIR) — draws a horizontal filled bar starting at
     * the current (inline, baseline) cursor. Payload wire layout observed in
     * MO:DCA/P5 streams from AFPWorld, DOC1, Adobe Output:
     * <pre>
     *   [0..1] rule length in L-units
     *   [2]    style (0x00 = solid, 0x01 = ?)
     *   [3]    rule thickness in L-units (varies 0x0F for thin separator to
     *          0xE0 for filled bars)
     *   [4]    flags (usually 0x00)
     * </pre>
     * The IBM MO:DCA reference catalogues the PTOCA opcode as 0xE4 (SBI) for
     * some versions, but every MO:DCA/P5 producer we've inspected emits this
     * function class for actual rule drawing. See {@code docs/blind-test-afpworld.md}.
     */
    public static final int DRAW_I_AXIS_RULE = 0xE4;
    /**
     * PTOCA Draw B-axis Rule (DBR) — vertical filled bar with the same
     * 5-byte payload as DIR. Function class 0xE6.
     */
    public static final int DRAW_B_AXIS_RULE = 0xE6;
    /**
     * PTOCA Set Extended Color (SEC) — 13-byte payload in RGB color space
     * (mode 0x01). Lifts the current foreground color for subsequent TRN runs
     * and rules.
     */
    public static final int SET_EXTENDED_COLOR = 0x80;

    // Phase 3 additions — text orientation + underscore attribute.
    /** Set Text Orientation (STO) — 4-byte payload, I-axis + B-axis rotation. */
    public static final int SET_TEXT_ORIENTATION = 0xF6;
    /** Underscore Character (USC) — 1-byte flag controls whether TRN is underscored. */
    public static final int UNDERSCORE_CHARACTER = 0xF0 ^ 0xF0; // placeholder, see below
    /** Underscore uses 0x76 in the PTOCA spec (AFPC-0007). */
    public static final int UNDERSCORE = 0x76;

    private PtocaControlCode() {
    }
}
