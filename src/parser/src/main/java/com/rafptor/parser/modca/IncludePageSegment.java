package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

public record IncludePageSegment(StructuredFieldId id, String segmentName) implements AfpStructuredField {

    public static IncludePageSegment parse(RawStructuredField raw) {
        return new IncludePageSegment(raw.id(), ModcaUtil.decodeFirstName(raw.data(), 8));
    }
}
