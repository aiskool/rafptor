package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * BPT — Begin Presentation Text Object ({@code D3 A8 9B}).
 *
 * <p>Opens a Presentation Text Object envelope. Body typically carries a
 * single name triplet naming the PT resource. Rafptor only needs the name
 * to optionally associate subsequent PTX/PTD with a named resource; all
 * semantic content comes from the inner PTX blocks.
 */
public record BeginPresentationText(StructuredFieldId id, String name) implements AfpStructuredField {

    public static BeginPresentationText parse(RawStructuredField raw) {
        return new BeginPresentationText(raw.id(), ModcaUtil.decodeFirstName(raw.data(), 8));
    }
}
