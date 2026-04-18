package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * Begin Image Object (BIM) — structured field {@code 0xD3 0xA8 0xFB}.
 * The body is an 8-byte EBCDIC name identifying the image object. The image
 * payload accumulates between BIM and its matching {@link EndImageObject}.
 */
public record BeginImageObject(StructuredFieldId id, String name) implements AfpStructuredField {

    public static BeginImageObject parse(RawStructuredField raw) {
        String n = ModcaUtil.decodeName(raw.data(), 0, Math.min(8, raw.data().length));
        return new BeginImageObject(raw.id(), n);
    }
}
