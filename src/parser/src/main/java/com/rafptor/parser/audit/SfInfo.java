package com.rafptor.parser.audit;

/**
 * Metadata for a Structured Field in the AFP / MO:DCA architecture.
 *
 * <p>Used by {@link SfRegistry} to classify SFs encountered during parsing.
 * {@code implemented = true} means Rafptor has a dedicated parser that
 * extracts semantic content (text, image bytes, font info, …). A false value
 * means the parser recognises the SF identifier but either does not produce
 * any IR element for it or only uses it for structural bookkeeping.
 */
public record SfInfo(String mnemonic,
                     String description,
                     Category category,
                     boolean implemented) {

    /** Broad classification used in the audit report. */
    public enum Category {
        DOCUMENT,          // BDT, EDT, DDD, …
        PAGE,              // BPG, EPG
        PAGE_GROUP,        // BNG, ENG
        OBJECT_ENV,        // BAG/EAG, BOG/EOG, BIM/EIM, BFN/EFN …
        TEXT,              // PTX, BPT, EPT, PTD
        FONT,              // MCF, MDR, FNC, FND, FNI, FNP, CPC, CPI
        IMAGE,             // BIM/EIM, IPD/IRD, IID, IOC, IDD
        GRAPHIC,           // BGR/EGR, GAD, GDD
        BARCODE,           // BBC, EBC, BDA, BDD
        RESOURCE,          // BRG/ERG, BRS/ERS, BDG/EDG
        OVERLAY,           // BMO/EMO, BDG/EDG (page-overlay), MMO, IPO
        PAGE_SEGMENT,      // BPS/EPS, IPS
        INCLUDE,           // IOB, IPO, IPS
        DESCRIPTOR,        // PGD, PTD, OBD, MDD, IDD, GDD, FDD, CPC
        MAP,               // MCF, MDR, MFC, MGO, MIO, MMO, MBC, MCC, MPO, MPS, MSU
        ENV_CONTROL,       // MMC, MFC (medium modification)
        INDEX,             // TLE, BNG/ENG, IEL
        CONTAINER,         // BDG/EDG (data object), OCD, ODD, OBD
        MISC               // NOP, XMD, and anything not yet categorised
    }

    public static final SfInfo UNKNOWN = new SfInfo("UNKNOWN", "Unknown structured field", Category.MISC, false);

    public SfInfo {
        if (mnemonic == null || mnemonic.isBlank()) {
            throw new IllegalArgumentException("mnemonic must not be blank");
        }
        if (description == null) {
            description = "";
        }
        if (category == null) {
            category = Category.MISC;
        }
    }
}
