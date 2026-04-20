package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * Minimal record carrying a structured field id + the optional 8-byte name
 * at offset 0 of the body. Intended for Begin/End envelope-only SFs that
 * Rafptor recognises structurally but does not yet dispatch semantically.
 *
 * <p>Keeping them under a generic type means the byte accountant classifies
 * them as {@code ENVELOPE_ONLY} (or {@code PARSED_IGNORED}) instead of
 * falling through to {@link UnknownStructuredField}.
 */
public record GenericEnvelope(StructuredFieldId id, String name, int bodyLength)
        implements AfpStructuredField {

    public static GenericEnvelope parse(RawStructuredField raw) {
        return new GenericEnvelope(
                raw.id(),
                ModcaUtil.decodeFirstName(raw.data(), 8),
                raw.dataLength());
    }
}
