package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

public record IncludeObject(StructuredFieldId id, String objectName) implements AfpStructuredField {

    public static IncludeObject parse(RawStructuredField raw) {
        return new IncludeObject(raw.id(), ModcaUtil.decodeFirstName(raw.data(), 8));
    }
}
