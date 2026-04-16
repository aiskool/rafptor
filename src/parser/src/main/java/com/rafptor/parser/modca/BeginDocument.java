package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

public record BeginDocument(StructuredFieldId id, String documentName) implements AfpStructuredField {

    public static BeginDocument parse(RawStructuredField raw) {
        String name = ModcaUtil.decodeFirstName(raw.data(), 8);
        return new BeginDocument(raw.id(), name);
    }
}
