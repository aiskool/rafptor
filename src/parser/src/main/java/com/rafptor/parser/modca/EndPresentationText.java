package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * EPT — End Presentation Text Object ({@code D3 A9 9B}). Closes the
 * envelope opened by {@link BeginPresentationText}.
 */
public record EndPresentationText(StructuredFieldId id, String name) implements AfpStructuredField {

    public static EndPresentationText parse(RawStructuredField raw) {
        return new EndPresentationText(raw.id(), ModcaUtil.decodeFirstName(raw.data(), 8));
    }
}
