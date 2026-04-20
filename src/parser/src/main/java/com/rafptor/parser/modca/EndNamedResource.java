package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * ERS — End Resource ({@code D3 A9 A5}). Matches the {@link BeginNamedResource}
 * (BRS) envelope used by MO:DCA/P5 composers to wrap JPEG/PNG/PDF payloads
 * inside the stream. Rafptor only needs the name to close the current
 * {@code currentNamedResource} scope in the parser loop.
 */
public record EndNamedResource(StructuredFieldId id, String resourceName) implements AfpStructuredField {

    public static EndNamedResource parse(RawStructuredField raw) {
        return new EndNamedResource(raw.id(), ModcaUtil.decodeFirstName(raw.data(), 8));
    }
}
