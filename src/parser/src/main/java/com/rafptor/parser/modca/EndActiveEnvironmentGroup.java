package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

public record EndActiveEnvironmentGroup(StructuredFieldId id, String name) implements AfpStructuredField {

    public static EndActiveEnvironmentGroup parse(RawStructuredField raw) {
        return new EndActiveEnvironmentGroup(raw.id(), ModcaUtil.decodeFirstName(raw.data(), 8));
    }
}
