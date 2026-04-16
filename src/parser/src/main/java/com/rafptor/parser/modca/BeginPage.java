package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

public record BeginPage(StructuredFieldId id, String pageName) implements AfpStructuredField {

    public static BeginPage parse(RawStructuredField raw) {
        String name = ModcaUtil.decodeFirstName(raw.data(), 8);
        return new BeginPage(raw.id(), name);
    }
}
