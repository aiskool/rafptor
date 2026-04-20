package com.rafptor.parser.reader;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import com.rafptor.parser.modca.BeginActiveEnvironmentGroup;
import com.rafptor.parser.modca.BeginDocument;
import com.rafptor.parser.modca.BeginNamedResource;
import com.rafptor.parser.modca.BeginObjectEnvironmentGroup;
import com.rafptor.parser.modca.BeginPresentationText;
import com.rafptor.parser.modca.EndNamedResource;
import com.rafptor.parser.modca.EndPresentationText;
import com.rafptor.parser.modca.BeginPage;
import com.rafptor.parser.modca.BeginResourceGroup;
import com.rafptor.parser.modca.EmbeddedObjectData;
import com.rafptor.parser.modca.GenericEnvelope;
import com.rafptor.parser.modca.EndActiveEnvironmentGroup;
import com.rafptor.parser.modca.EndDocument;
import com.rafptor.parser.modca.EndObjectEnvironmentGroup;
import com.rafptor.parser.modca.EndPage;
import com.rafptor.parser.modca.EndResourceGroup;
import com.rafptor.parser.modca.FontIndex;
import com.rafptor.parser.modca.IncludeObject;
import com.rafptor.parser.modca.IncludePageOverlay;
import com.rafptor.parser.modca.IncludePageSegment;
import com.rafptor.parser.modca.MapCodedFont;
import com.rafptor.parser.modca.BeginGraphicsObject;
import com.rafptor.parser.modca.BeginImageObject;
import com.rafptor.parser.modca.EndGraphicsObject;
import com.rafptor.parser.modca.GraphicsData;
import com.rafptor.parser.modca.EndImageObject;
import com.rafptor.parser.modca.ImageRasterData;
import com.rafptor.parser.modca.MapDataResource;
import com.rafptor.parser.modca.NoOperation;
import com.rafptor.parser.modca.PageDescriptor;
import com.rafptor.parser.modca.PresentationTextData;
import com.rafptor.parser.modca.PresentationTextDescriptor;
import com.rafptor.parser.modca.TagLogicalElement;
import com.rafptor.parser.modca.UnknownStructuredField;

import java.util.Map;
import java.util.function.Function;

/**
 * Dispatches a {@link RawStructuredField} to its typed parser.
 *
 * <p>Unknown Structured Fields are NOT rejected — they are wrapped in
 * {@link UnknownStructuredField} so that the pipeline remains tolerant of AFP
 * extensions and client-specific records.
 */
public final class StructuredFieldReader {

    private static final Map<Integer, Function<RawStructuredField, AfpStructuredField>> DISPATCH = Map.ofEntries(
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xA8).toInt(), BeginDocument::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xA8).toInt(), EndDocument::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xAF).toInt(), BeginPage::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xAF).toInt(), EndPage::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xC9).toInt(), BeginActiveEnvironmentGroup::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xC9).toInt(), EndActiveEnvironmentGroup::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xC6).toInt(), BeginResourceGroup::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xC6).toInt(), EndResourceGroup::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xC7).toInt(), BeginObjectEnvironmentGroup::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xC7).toInt(), EndObjectEnvironmentGroup::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xAB, 0x8A).toInt(), MapCodedFont::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xAB, 0xC3).toInt(), MapDataResource::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xAF, 0xD8).toInt(), IncludePageOverlay::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xAF, 0x5F).toInt(), IncludePageSegment::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xAF, 0xC3).toInt(), IncludeObject::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA0, 0x90).toInt(), TagLogicalElement::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xEE, 0xEE).toInt(), NoOperation::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xEE, 0x9B).toInt(), PresentationTextData::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA6, 0xAF).toInt(), PageDescriptor::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xB1, 0x9B).toInt(), PresentationTextDescriptor::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xFB).toInt(), BeginImageObject::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xFB).toInt(), EndImageObject::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xEE, 0xFB).toInt(), ImageRasterData::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xBB).toInt(), BeginGraphicsObject::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xBB).toInt(), EndGraphicsObject::parse),
            Map.entry(StructuredFieldId.of(0xD3, 0xEE, 0xBB).toInt(), GraphicsData::parse),
            // Named-resource wrappers used by MO:DCA/P5 composers to embed
            // JPEG/PNG logos and other object containers inside the stream.
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xA5).toInt(), BeginNamedResource::parse), // BRS
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xCE).toInt(), BeginNamedResource::parse), // BFN
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xCE).toInt(), GenericEnvelope::parse),    // EFN
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0x92).toInt(), BeginNamedResource::parse), // BDG
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0x92).toInt(), GenericEnvelope::parse),    // EDG
            Map.entry(StructuredFieldId.of(0xD3, 0xEE, 0x92).toInt(), EmbeddedObjectData::parse), // raw object bytes
            // Presentation Text envelope + End-Resource — BPT / EPT / ERS are
            // emitted by every MO:DCA-P5 producer but were previously falling
            // through to UnknownStructuredField. Wiring them restores the
            // dispatch coverage caught by the byte-accounting audit.
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0x9B).toInt(), BeginPresentationText::parse), // BPT
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0x9B).toInt(), EndPresentationText::parse),   // EPT
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xA5).toInt(), EndNamedResource::parse),      // ERS
            // ---- Phase 1: every remaining envelope Begin/End pair that the
            // MO:DCA spec defines is wired to GenericEnvelope so the byte
            // accountant can classify them as ENVELOPE_ONLY rather than
            // UNKNOWN. Each entry is a 3-byte SF identifier; the paired
            // End/Begin lives at the symmetrical (0xA9 vs 0xA8) value.
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xAD).toInt(), GenericEnvelope::parse), // BNG Begin Named Page Group
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xAD).toInt(), GenericEnvelope::parse), // ENG
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xDF).toInt(), GenericEnvelope::parse), // BMO Begin Medium Overlay
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xDF).toInt(), GenericEnvelope::parse), // EMO
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0x5F).toInt(), GenericEnvelope::parse), // BPS Begin Page Segment
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0x5F).toInt(), GenericEnvelope::parse), // EPS
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0x6B).toInt(), GenericEnvelope::parse), // BOC Begin Object Container (alt)
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0x6B).toInt(), GenericEnvelope::parse), // EOC
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xEB).toInt(), GenericEnvelope::parse), // BBC Begin Barcode Object
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xEB).toInt(), GenericEnvelope::parse), // EBC
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0x8A).toInt(), GenericEnvelope::parse), // BCF Begin Coded Font
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0x8A).toInt(), GenericEnvelope::parse), // ECF
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0x87).toInt(), GenericEnvelope::parse), // BCP Begin Code Page
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0x87).toInt(), GenericEnvelope::parse), // ECP
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xCD).toInt(), GenericEnvelope::parse), // BFM Begin Form Map (FormDef)
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xCD).toInt(), GenericEnvelope::parse), // EFM
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xBA).toInt(), GenericEnvelope::parse), // BPF Begin Page Map (PageDef)
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xBA).toInt(), GenericEnvelope::parse), // EPF
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0xDD).toInt(), GenericEnvelope::parse), // BMM Begin Medium Map
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0xDD).toInt(), GenericEnvelope::parse), // EMM
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0x9A).toInt(), GenericEnvelope::parse), // BSG Begin Resource Environment Group
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0x9A).toInt(), GenericEnvelope::parse), // ESG
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0x8D).toInt(), GenericEnvelope::parse), // BDM Begin Data Map
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0x8D).toInt(), GenericEnvelope::parse), // EDM
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0x7B).toInt(), GenericEnvelope::parse), // BII Begin IM Image (legacy)
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0x7B).toInt(), GenericEnvelope::parse), // EII
            Map.entry(StructuredFieldId.of(0xD3, 0xA8, 0x77).toInt(), GenericEnvelope::parse), // BAA Begin Attribute Area
            Map.entry(StructuredFieldId.of(0xD3, 0xA9, 0x77).toInt(), GenericEnvelope::parse), // EAA
            // Phase 2: FNI (Font Index) dispatched for its per-character metrics.
            Map.entry(StructuredFieldId.of(0xD3, 0x8C, 0x89).toInt(), FontIndex::parse)
    );

    /** True if the dispatcher has an entry for this SF id — used by tests. */
    public static boolean isDispatchable(int idInt) {
        return DISPATCH.containsKey(idInt);
    }

    public static boolean isDispatchable(String idHex) {
        if (idHex == null || idHex.length() < 6) return false;
        int v = Integer.parseInt(idHex.substring(0, 6), 16);
        return DISPATCH.containsKey(v);
    }

    private StructuredFieldReader() {
    }

    public static AfpStructuredField dispatch(RawStructuredField raw) {
        if (raw == null) {
            throw new IllegalArgumentException("raw must not be null");
        }
        Function<RawStructuredField, AfpStructuredField> parser = DISPATCH.get(raw.id().toInt());
        if (parser == null) {
            return UnknownStructuredField.of(raw);
        }
        return parser.apply(raw);
    }
}
