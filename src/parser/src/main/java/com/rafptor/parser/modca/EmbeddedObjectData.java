package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * Structured field {@code 0xD3 0xEE 0x92} used by some MO:DCA/P5 composers
 * (AFPWorld, certain IBM PPFA tooling) to carry the raw bytes of an embedded
 * object resource — typically a JPEG logo or an overlay image. The bytes are
 * written directly into the SF body with no additional envelope, so the magic
 * bytes (FFD8FF = JPEG, 89504E47 = PNG, etc.) appear at offset 0.
 *
 * <p>The data is captured verbatim and detection of the image kind is left to
 * the image decoder downstream.
 */
public record EmbeddedObjectData(StructuredFieldId id, byte[] payload,
                                 Kind kind) implements AfpStructuredField {

    public EmbeddedObjectData {
        if (payload == null) throw new IllegalArgumentException("payload must not be null");
        payload = payload.clone();
        if (kind == null) kind = Kind.UNKNOWN;
    }

    public enum Kind { JPEG, PNG, TIFF, UNKNOWN }

    public static EmbeddedObjectData parse(RawStructuredField raw) {
        byte[] data = raw.data();
        return new EmbeddedObjectData(raw.id(), data, detect(data));
    }

    private static Kind detect(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return Kind.JPEG;
        }
        if (d.length >= 4 && (d[0] & 0xFF) == 0x89
                && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return Kind.PNG;
        }
        if (d.length >= 4 && ((d[0] & 0xFF) == 0x49 && d[1] == 'I'
                || (d[0] & 0xFF) == 0x4D && d[1] == 'M')) {
            return Kind.TIFF;
        }
        return Kind.UNKNOWN;
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }
}
