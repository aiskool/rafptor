package com.rafptor.parser.reader;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import com.rafptor.parser.modca.BeginActiveEnvironmentGroup;
import com.rafptor.parser.modca.BeginDocument;
import com.rafptor.parser.modca.BeginObjectEnvironmentGroup;
import com.rafptor.parser.modca.BeginPage;
import com.rafptor.parser.modca.BeginResourceGroup;
import com.rafptor.parser.modca.EndActiveEnvironmentGroup;
import com.rafptor.parser.modca.EndDocument;
import com.rafptor.parser.modca.EndObjectEnvironmentGroup;
import com.rafptor.parser.modca.EndPage;
import com.rafptor.parser.modca.EndResourceGroup;
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
            Map.entry(StructuredFieldId.of(0xD3, 0xEE, 0xBB).toInt(), GraphicsData::parse)
    );

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
