package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

public record BeginActiveEnvironmentGroup(StructuredFieldId id, String name) implements AfpStructuredField {

    public static BeginActiveEnvironmentGroup parse(RawStructuredField raw) {
        return new BeginActiveEnvironmentGroup(raw.id(), ModcaUtil.decodeFirstName(raw.data(), 8));
    }
}
