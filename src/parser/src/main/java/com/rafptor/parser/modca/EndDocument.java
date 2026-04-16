package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

public record EndDocument(StructuredFieldId id, String documentName) implements AfpStructuredField {

    public static EndDocument parse(RawStructuredField raw) {
        String name = ModcaUtil.decodeFirstName(raw.data(), 8);
        return new EndDocument(raw.id(), name);
    }
}
