package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * Presentation Text Data (PTX) — wraps a PTOCA control sequence stream.
 * The payload is handed off to {@link com.rafptor.parser.ptoca.PtocaParser}.
 */
public record PresentationTextData(StructuredFieldId id, byte[] payload) implements AfpStructuredField {

    public PresentationTextData {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        payload = payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public int payloadLength() {
        return payload.length;
    }

    public static PresentationTextData parse(RawStructuredField raw) {
        return new PresentationTextData(raw.id(), raw.data());
    }
}
