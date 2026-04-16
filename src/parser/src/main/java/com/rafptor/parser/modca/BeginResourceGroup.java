package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

public record BeginResourceGroup(StructuredFieldId id, String name) implements AfpStructuredField {

    public static BeginResourceGroup parse(RawStructuredField raw) {
        return new BeginResourceGroup(raw.id(), ModcaUtil.decodeFirstName(raw.data(), 8));
    }
}
