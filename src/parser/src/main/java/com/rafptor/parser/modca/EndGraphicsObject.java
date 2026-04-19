package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * End Graphics Object (EGR) — structured field {@code 0xD3 0xA9 0xBB}. Closes
 * a {@link BeginGraphicsObject} envelope.
 */
public record EndGraphicsObject(StructuredFieldId id, String name) implements AfpStructuredField {

    public static EndGraphicsObject parse(RawStructuredField raw) {
        String n = ModcaUtil.decodeName(raw.data(), 0, Math.min(8, raw.data().length));
        return new EndGraphicsObject(raw.id(), n);
    }
}
