package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * End Image Object (EIM) — structured field {@code 0xD3 0xA9 0xFB}.
 */
public record EndImageObject(StructuredFieldId id, String name) implements AfpStructuredField {

    public static EndImageObject parse(RawStructuredField raw) {
        String n = ModcaUtil.decodeName(raw.data(), 0, Math.min(8, raw.data().length));
        return new EndImageObject(raw.id(), n);
    }
}
