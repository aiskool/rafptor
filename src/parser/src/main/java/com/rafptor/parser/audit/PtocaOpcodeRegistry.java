package com.rafptor.parser.audit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.rafptor.parser.audit.OpcodeInfo.Category.*;

/**
 * Registry of PTOCA control-sequence opcodes. PTOCA uses a chaining flag in
 * the low bit of the function-class byte (LSB = 1 signals that another CS
 * follows in the chained form), so lookups are done on the masked value
 * {@code op & 0xFE}.
 *
 * <p>Sources: PTOCA Reference AFPC-0007-03 (AFP Consortium), observed values
 * in the Rafptor corpus and in the Apache FOP AFP renderer.
 */
public final class PtocaOpcodeRegistry {

    private static final Map<Integer, OpcodeInfo> REGISTRY;

    static {
        LinkedHashMap<Integer, OpcodeInfo> m = new LinkedHashMap<>();

        // Positioning
        m.put(0xC0, new OpcodeInfo("SBP", "Set Baseline Position", POSITIONING, false));
        m.put(0xC2, new OpcodeInfo("SBI", "Set Baseline Increment", POSITIONING, false));
        m.put(0xC4, new OpcodeInfo("SIM", "Set Inline Margin", POSITIONING, false));
        m.put(0xC6, new OpcodeInfo("AMI", "Absolute Move Inline", POSITIONING, true));
        m.put(0xC8, new OpcodeInfo("RMI", "Relative Move Inline", POSITIONING, true));
        m.put(0xD0, new OpcodeInfo("SVI", "Set Variable Space Increment", SPACING, false));
        m.put(0xD2, new OpcodeInfo("AMB", "Absolute Move Baseline", POSITIONING, true));
        m.put(0xD4, new OpcodeInfo("RMB", "Relative Move Baseline", POSITIONING, false));

        // Font / orientation
        m.put(0xF6, new OpcodeInfo("STO", "Set Text Orientation", FONT, true));
        m.put(0xF0, new OpcodeInfo("SCFL", "Set Coded Font Local", FONT, true));

        // Spacing
        m.put(0xC0 & 0xFE, new OpcodeInfo("SBP", "Set Baseline Position", POSITIONING, false));
        m.put(0xC8 & 0xFE, new OpcodeInfo("RMI", "Relative Move Inline", POSITIONING, true));

        // Color
        m.put(0x74, new OpcodeInfo("SCR", "Set Coloured Rule", COLOR, false));
        m.put(0x80, new OpcodeInfo("SEC", "Set Extended Color", COLOR, true));
        // STC (Set Text Color, 2-byte color index) is 0x72 in the PTOCA spec.
        m.put(0x72, new OpcodeInfo("STC", "Set Text Color", COLOR, false));

        // Rules
        m.put(0xE4, new OpcodeInfo("DIR", "Draw I-axis Rule", RULE, true));
        m.put(0xE6, new OpcodeInfo("DBR", "Draw B-axis Rule", RULE, true));

        // Text
        m.put(0xDA, new OpcodeInfo("TRN", "Transparent Data", TEXT, true));

        // Underscoring / control
        m.put(0x76, new OpcodeInfo("USC", "Underscore Character", CONTROL, true));
        m.put(0xF2, new OpcodeInfo("BSU", "Begin Suppression", CONTROL, false));
        m.put(0xF4, new OpcodeInfo("ESU", "End Suppression", CONTROL, false));
        m.put(0xF8, new OpcodeInfo("BLN", "Begin Line", CONTROL, false));
        m.put(0x00, new OpcodeInfo("NOP", "No Operation / filler", CONTROL, true));
        m.put(0xFA, new OpcodeInfo("TBM", "Temporary Baseline Move", CONTROL, false));

        // Spacing
        m.put(0xC4 & 0xFE, new OpcodeInfo("SIM", "Set Inline Margin", POSITIONING, false));
        m.put(0xC0 | 0x00, new OpcodeInfo("SIA", "Set Intercharacter Adjustment", SPACING, false));

        REGISTRY = Collections.unmodifiableMap(m);
    }

    private PtocaOpcodeRegistry() {
    }

    /**
     * Lookup by the <em>masked</em> opcode value {@code op & 0xFE}, since the
     * low bit is the PTOCA chaining flag.
     */
    public static Optional<OpcodeInfo> lookup(int opcodeMasked) {
        return Optional.ofNullable(REGISTRY.get(opcodeMasked & 0xFE));
    }

    public static boolean isImplemented(int opcodeMasked) {
        return lookup(opcodeMasked).map(OpcodeInfo::implemented).orElse(false);
    }

    public static int size() {
        return REGISTRY.size();
    }

    public static Map<Integer, OpcodeInfo> entries() {
        return REGISTRY;
    }
}
