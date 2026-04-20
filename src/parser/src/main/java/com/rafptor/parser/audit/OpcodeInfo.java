package com.rafptor.parser.audit;

/**
 * Metadata for a PTOCA control sequence opcode. See
 * {@link PtocaOpcodeRegistry} for the full table.
 */
public record OpcodeInfo(String mnemonic,
                         String description,
                         Category category,
                         boolean implemented) {

    public enum Category {
        POSITIONING,   // AMB, AMI, RMB, RMI, SBI, SBP, SIM
        FONT,          // SCFL, STO
        COLOR,         // STC, SEC, SCR
        RULE,          // DIR, DBR
        TEXT,          // TRN
        SPACING,       // SIA, SVI
        CONTROL,       // USC, BSU, ESU, NOP, BLN
        OTHER
    }

    public static final OpcodeInfo UNKNOWN = new OpcodeInfo("UNKNOWN", "Unknown opcode", Category.OTHER, false);

    public OpcodeInfo {
        if (mnemonic == null || mnemonic.isBlank()) {
            throw new IllegalArgumentException("mnemonic must not be blank");
        }
        if (description == null) {
            description = "";
        }
        if (category == null) {
            category = Category.OTHER;
        }
    }
}
