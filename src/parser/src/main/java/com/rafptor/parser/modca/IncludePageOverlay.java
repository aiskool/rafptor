package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

public record IncludePageOverlay(StructuredFieldId id, String overlayName) implements AfpStructuredField {

    public static IncludePageOverlay parse(RawStructuredField raw) {
        return new IncludePageOverlay(raw.id(), ModcaUtil.decodeFirstName(raw.data(), 8));
    }
}
