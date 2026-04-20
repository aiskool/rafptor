package com.rafptor.parser.audit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.rafptor.parser.audit.SfInfo.Category.*;

/**
 * Exhaustive registry of MO:DCA / IOCA / GOCA / BCOCA / FOCA / PTOCA structured
 * fields. Built from the AFP Consortium specifications (MO:DCA AFPC-0004,
 * IOCA AFPC-0009, GOCA AFPC-0008, BCOCA AFPC-0003, FOCA AFPC-0006) and from
 * the structured fields observed across the Rafptor test corpus.
 *
 * <p>Each entry is keyed by its 6-character hex SF identifier (upper-case,
 * no separators). The {@link SfInfo#implemented()} flag indicates whether
 * Rafptor currently produces IR content from the SF (vs. just recognising
 * it and moving on).
 *
 * <p>Adding a new implementation to Rafptor: flip the {@code implemented}
 * flag to {@code true} in this registry at the same time as wiring the
 * dispatcher — the audit CLI will then report improved coverage.
 */
public final class SfRegistry {

    private static final Map<String, SfInfo> REGISTRY;

    static {
        LinkedHashMap<String, SfInfo> m = new LinkedHashMap<>();

        // ---- Document / Envelope ----
        m.put("D3A8A8", new SfInfo("BDT", "Begin Document", DOCUMENT, true));
        m.put("D3A9A8", new SfInfo("EDT", "End Document", DOCUMENT, true));
        m.put("D3A6A8", new SfInfo("DXD", "Document Environment Descriptor", DESCRIPTOR, false));

        // ---- Page Group ----
        m.put("D3A8AD", new SfInfo("BNG", "Begin Named Page Group", PAGE_GROUP, true));
        m.put("D3A9AD", new SfInfo("ENG", "End Named Page Group", PAGE_GROUP, true));

        // ---- Page ----
        m.put("D3A8AF", new SfInfo("BPG", "Begin Page", PAGE, true));
        m.put("D3A9AF", new SfInfo("EPG", "End Page", PAGE, true));
        m.put("D3A6AF", new SfInfo("PGD", "Page Descriptor", DESCRIPTOR, true));
        m.put("D3B1AF", new SfInfo("PGP", "Page Position", DESCRIPTOR, false));

        // ---- Active Environment Group (AEG) ----
        m.put("D3A8C9", new SfInfo("BAG", "Begin Active Environment Group", OBJECT_ENV, true));
        m.put("D3A9C9", new SfInfo("EAG", "End Active Environment Group", OBJECT_ENV, true));

        // ---- Object Environment Group (OEG) ----
        m.put("D3A8C7", new SfInfo("BOG", "Begin Object Environment Group", OBJECT_ENV, true));
        m.put("D3A9C7", new SfInfo("EOG", "End Object Environment Group", OBJECT_ENV, true));
        m.put("D3A7C7", new SfInfo("OBD", "Object Area Descriptor", DESCRIPTOR, false));
        m.put("D3AC7C", new SfInfo("OBP", "Object Area Position", DESCRIPTOR, false));

        // ---- Resource Group ----
        m.put("D3A8C6", new SfInfo("BRG", "Begin Resource Group", RESOURCE, true));
        m.put("D3A9C6", new SfInfo("ERG", "End Resource Group", RESOURCE, true));

        // ---- Named Resource (BRS / ERS — a single resource inside a BRG) ----
        m.put("D3A8A5", new SfInfo("BRS", "Begin Resource", RESOURCE, true));
        m.put("D3A9A5", new SfInfo("ERS", "End Resource", RESOURCE, true));

        // ---- Presentation Text Object ----
        m.put("D3A89B", new SfInfo("BPT", "Begin Presentation Text Object", TEXT, true));
        m.put("D3A99B", new SfInfo("EPT", "End Presentation Text Object", TEXT, true));
        m.put("D3B19B", new SfInfo("PTD", "Presentation Text Data Descriptor", DESCRIPTOR, true));
        m.put("D3EE9B", new SfInfo("PTX", "Presentation Text Data", TEXT, true));

        // ---- Image Object ----
        m.put("D3A8FB", new SfInfo("BIM", "Begin Image Object", IMAGE, true));
        m.put("D3A9FB", new SfInfo("EIM", "End Image Object", IMAGE, true));
        m.put("D3EEFB", new SfInfo("IRD", "Image Raster Data (IOCA self-defining data)", IMAGE, true));
        m.put("D3AEFB", new SfInfo("MIO", "Map IO-Image", MAP, false));
        m.put("D3A6FB", new SfInfo("IDD", "Image Data Descriptor", DESCRIPTOR, false));
        m.put("D3ABFB", new SfInfo("IID", "Image Input Descriptor (IM-Image)", DESCRIPTOR, false));

        // ---- Graphics Object (GOCA) ----
        m.put("D3A8BB", new SfInfo("BGR", "Begin Graphics Object", GRAPHIC, true));
        m.put("D3A9BB", new SfInfo("EGR", "End Graphics Object", GRAPHIC, true));
        m.put("D3EEBB", new SfInfo("GAD", "Graphics Data", GRAPHIC, true));
        m.put("D3A6BB", new SfInfo("GDD", "Graphics Data Descriptor", DESCRIPTOR, false));
        m.put("D3ABBB", new SfInfo("MGO", "Map Graphics Object", MAP, false));

        // ---- Barcode Object (BCOCA) ----
        m.put("D3A8EB", new SfInfo("BBC", "Begin Barcode Object", BARCODE, false));
        m.put("D3A9EB", new SfInfo("EBC", "End Barcode Object", BARCODE, false));
        m.put("D3EEEB", new SfInfo("BDA", "Barcode Data", BARCODE, false));
        m.put("D3A6EB", new SfInfo("BDD", "Barcode Data Descriptor", DESCRIPTOR, false));
        m.put("D3ABEB", new SfInfo("MBC", "Map Barcode Object", MAP, false));
        // Legacy 6B class observed in xafp corpus bank/letter samples —
        // placement/descriptor records paired with GOCA map entries.
        m.put("D3AC6B", new SfInfo("OBP3", "Object Area Position (legacy 6B class)", DESCRIPTOR, false));
        m.put("D3A66B", new SfInfo("OBD3", "Object Area Descriptor (legacy 6B class)", DESCRIPTOR, false));

        // ---- Object Container (generic — JPEG / PNG / PDF embeds) ----
        m.put("D3A892", new SfInfo("BDG", "Begin Data Object / Document Group", CONTAINER, true));
        m.put("D3A992", new SfInfo("EDG", "End Data Object / Document Group", CONTAINER, true));
        m.put("D3EE92", new SfInfo("OCD", "Object Container Data (raw JPEG/PNG/TIFF)", CONTAINER, true));
        m.put("D3A692", new SfInfo("OBD", "Object Container Descriptor", DESCRIPTOR, false));
        m.put("D3A792", new SfInfo("OBC", "Object Container Control", DESCRIPTOR, false));
        m.put("D3AC92", new SfInfo("MPO", "Map Page Overlay / Object", MAP, false));

        // ---- Font Object (FOCA) ----
        m.put("D3A8CE", new SfInfo("BFN", "Begin Font Object", FONT, true));
        m.put("D3A9CE", new SfInfo("EFN", "End Font Object", FONT, true));
        m.put("D3A689", new SfInfo("FND", "Font Descriptor", FONT, false));
        m.put("D3A789", new SfInfo("FNC", "Font Control", FONT, false));
        m.put("D38C89", new SfInfo("FNI", "Font Index", FONT, true));
        m.put("D38E89", new SfInfo("FNP", "Font Patterns", FONT, false));
        m.put("D3EE89", new SfInfo("FNG", "Font Pattern Data (raster glyph body)", FONT, false));
        m.put("D3AB89", new SfInfo("MCF1", "Map Coded Font (Format 1 alt)", FONT, false));
        m.put("D3AE89", new SfInfo("FNN", "Font Name Map", FONT, false));
        m.put("D3AC89", new SfInfo("MFC", "Map Font Coordinate / FNC-alt", FONT, false));
        m.put("D3878A", new SfInfo("FNO", "Font Orientation", FONT, false));
        m.put("D3AC8A", new SfInfo("CPC", "Code Page Control", FONT, false));
        m.put("D38C8A", new SfInfo("CPI", "Code Page Index", FONT, false));
        m.put("D3A68A", new SfInfo("CPD", "Code Page Descriptor", FONT, false));
        m.put("D3B18A", new SfInfo("CPT", "Code Page Text / Bitmap Data", FONT, false));
        m.put("D3AB8A", new SfInfo("MCF", "Map Coded Font (Format 1)", FONT, true));
        m.put("D3B188", new SfInfo("MCF2", "Map Coded Font (Format 2)", FONT, false));

        // ---- Map Data Resource ----
        m.put("D3ABC3", new SfInfo("MCF_ALT", "Map Coded Font (AFPC alt encoding)", FONT, true));
        m.put("D3AFC3", new SfInfo("MDR", "Map Data Resource", MAP, true));

        // ---- Medium ----
        m.put("D3A88A", new SfInfo("BMM", "Begin Medium Map", ENV_CONTROL, false));
        m.put("D3A98A", new SfInfo("EMM", "End Medium Map", ENV_CONTROL, false));
        m.put("D3A788", new SfInfo("MMC", "Medium Modification Control", ENV_CONTROL, false));
        m.put("D3A688", new SfInfo("MDD", "Medium Descriptor", DESCRIPTOR, false));
        m.put("D3B088", new SfInfo("MMD", "Medium Map Descriptor", DESCRIPTOR, false));

        // ---- Overlay ----
        m.put("D3A8DF", new SfInfo("BMO", "Begin Medium Overlay", OVERLAY, true));
        m.put("D3A9DF", new SfInfo("EMO", "End Medium Overlay", OVERLAY, true));
        m.put("D3B1DF", new SfInfo("OVD", "Overlay Descriptor", DESCRIPTOR, false));
        m.put("D3AFD8", new SfInfo("IPO", "Include Page Overlay", INCLUDE, true));
        m.put("D3ABD8", new SfInfo("MPO", "Map Page Overlay", MAP, false));
        m.put("D3ACD8", new SfInfo("MMO2", "Map Medium Overlay (alt)", MAP, false));

        // ---- Page Segment ----
        m.put("D3A85F", new SfInfo("BPS", "Begin Page Segment", PAGE_SEGMENT, true));
        m.put("D3A95F", new SfInfo("EPS", "End Page Segment", PAGE_SEGMENT, true));
        m.put("D3AF5F", new SfInfo("IPS", "Include Page Segment", INCLUDE, true));
        m.put("D3B15F", new SfInfo("PSD", "Page Segment Descriptor", DESCRIPTOR, false));
        m.put("D3AC5F", new SfInfo("MPS", "Map Page Segment", MAP, false));

        // ---- Include Object ----
        m.put("D3AFC5", new SfInfo("IOB", "Include Object", INCLUDE, true));
        m.put("D3ACC5", new SfInfo("MMO", "Map Medium Overlay", MAP, false));

        // ---- Index / metadata ----
        m.put("D3A090", new SfInfo("TLE", "Tag Logical Element", INDEX, true));
        m.put("D3A190", new SfInfo("IEL", "Index Element", INDEX, false));
        m.put("D3B290", new SfInfo("IMM", "Invoke Medium Map", ENV_CONTROL, false));
        m.put("D3A290", new SfInfo("BII", "Begin Index Index", INDEX, false));
        m.put("D3A390", new SfInfo("EII", "End Index Index", INDEX, false));

        // ---- Resource-group-level housekeeping ----
        m.put("D3EEEE", new SfInfo("NOP", "No Operation", MISC, true));

        // ---- Color / presentation management ----
        m.put("D3B1C3", new SfInfo("CMT", "Color Management Table", DESCRIPTOR, false));
        m.put("D3A7CA", new SfInfo("CAT", "Color Attribute Table", DESCRIPTOR, false));
        m.put("D3A69B", new SfInfo("PTDX", "Presentation Text Descriptor Extension", DESCRIPTOR, false));
        m.put("D3A79B", new SfInfo("PTC", "Presentation Text Control", ENV_CONTROL, false));

        // ---- Optional / rarely observed ----
        m.put("D3B2C9", new SfInfo("PEC", "Presentation Environment Control", ENV_CONTROL, false));
        m.put("D3ABA9", new SfInfo("MPG", "Map Page", MAP, false));

        // ---- Phase 1: additional envelope pairs wired via GenericEnvelope ----
        m.put("D3A86B", new SfInfo("BOC", "Begin Object Container (alt)", CONTAINER, true));
        m.put("D3A96B", new SfInfo("EOC", "End Object Container (alt)", CONTAINER, true));
        m.put("D3A8EB", new SfInfo("BBC", "Begin Barcode Object", BARCODE, true));
        m.put("D3A9EB", new SfInfo("EBC", "End Barcode Object", BARCODE, true));
        m.put("D3A88A", new SfInfo("BCF", "Begin Coded Font", FONT, true));
        m.put("D3A98A", new SfInfo("ECF", "End Coded Font", FONT, true));
        m.put("D3A887", new SfInfo("BCP", "Begin Code Page", FONT, true));
        m.put("D3A987", new SfInfo("ECP", "End Code Page", FONT, true));
        m.put("D3A8CD", new SfInfo("BFM", "Begin Form Map (FormDef)", ENV_CONTROL, true));
        m.put("D3A9CD", new SfInfo("EFM", "End Form Map", ENV_CONTROL, true));
        m.put("D3A8BA", new SfInfo("BPF", "Begin Page Map (PageDef)", ENV_CONTROL, true));
        m.put("D3A9BA", new SfInfo("EPF", "End Page Map", ENV_CONTROL, true));
        m.put("D3A8DD", new SfInfo("BMM", "Begin Medium Map", ENV_CONTROL, true));
        m.put("D3A9DD", new SfInfo("EMM", "End Medium Map", ENV_CONTROL, true));
        m.put("D3A89A", new SfInfo("BSG", "Begin Resource Environment Group", RESOURCE, true));
        m.put("D3A99A", new SfInfo("ESG", "End Resource Environment Group", RESOURCE, true));
        m.put("D3A88D", new SfInfo("BDM", "Begin Data Map", ENV_CONTROL, true));
        m.put("D3A98D", new SfInfo("EDM", "End Data Map", ENV_CONTROL, true));
        m.put("D3A87B", new SfInfo("BII", "Begin IM Image (legacy)", IMAGE, true));
        m.put("D3A97B", new SfInfo("EII", "End IM Image (legacy)", IMAGE, true));
        m.put("D3A877", new SfInfo("BAA", "Begin Attribute Area", ENV_CONTROL, true));
        m.put("D3A977", new SfInfo("EAA", "End Attribute Area", ENV_CONTROL, true));

        REGISTRY = Collections.unmodifiableMap(m);
    }

    private SfRegistry() {
    }

    /** @return the SF info if registered, else empty. */
    public static Optional<SfInfo> lookup(String idHex) {
        if (idHex == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(REGISTRY.get(idHex.toUpperCase()));
    }

    public static boolean isImplemented(String idHex) {
        return lookup(idHex).map(SfInfo::implemented).orElse(false);
    }

    public static int size() {
        return REGISTRY.size();
    }

    /** Unmodifiable view — useful for audit reports that iterate the whole table. */
    public static Map<String, SfInfo> entries() {
        return REGISTRY;
    }
}
