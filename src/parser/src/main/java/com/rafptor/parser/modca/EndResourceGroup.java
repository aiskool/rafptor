package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

public record EndResourceGroup(StructuredFieldId id, String name) implements AfpStructuredField {

    public static EndResourceGroup parse(RawStructuredField raw) {
        return new EndResourceGroup(raw.id(), ModcaUtil.decodeFirstName(raw.data(), 8));
    }
}
