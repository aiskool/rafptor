package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

public record EndObjectEnvironmentGroup(StructuredFieldId id, String name) implements AfpStructuredField {

    public static EndObjectEnvironmentGroup parse(RawStructuredField raw) {
        return new EndObjectEnvironmentGroup(raw.id(), ModcaUtil.decodeFirstName(raw.data(), 8));
    }
}
