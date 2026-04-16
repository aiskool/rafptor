package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * Placeholder for Structured Fields we do not (yet) parse. Preserved in the
 * document AST so the conversion pipeline can account for them.
 */
public record UnknownStructuredField(StructuredFieldId id, int flags, int dataLength) implements AfpStructuredField {

    public static UnknownStructuredField of(RawStructuredField raw) {
        return new UnknownStructuredField(raw.id(), raw.flags(), raw.dataLength());
    }
}
