package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

public record EndPage(StructuredFieldId id, String pageName) implements AfpStructuredField {

    public static EndPage parse(RawStructuredField raw) {
        String name = ModcaUtil.decodeFirstName(raw.data(), 8);
        return new EndPage(raw.id(), name);
    }
}
