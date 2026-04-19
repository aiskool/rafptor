package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * Begin Graphics Object (BGR) — structured field {@code 0xD3 0xA8 0xBB}. Marks
 * the start of a GOCA graphic segment inside a page. The body holds the
 * 8-byte resource name in EBCDIC; drawing orders arrive in subsequent
 * Graphics Data (GAD) structured fields until End Graphics Object (EGR).
 */
public record BeginGraphicsObject(StructuredFieldId id, String name) implements AfpStructuredField {

    public static BeginGraphicsObject parse(RawStructuredField raw) {
        String n = ModcaUtil.decodeName(raw.data(), 0, Math.min(8, raw.data().length));
        return new BeginGraphicsObject(raw.id(), n);
    }
}
