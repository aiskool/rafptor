package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * No Operation (NOP) — opaque commentary or vendor metadata. Rafptor preserves
 * the length but NEVER logs or exposes the payload (may contain client data).
 */
public record NoOperation(StructuredFieldId id, int payloadLength) implements AfpStructuredField {

    public static NoOperation parse(RawStructuredField raw) {
        return new NoOperation(raw.id(), raw.dataLength());
    }
}
