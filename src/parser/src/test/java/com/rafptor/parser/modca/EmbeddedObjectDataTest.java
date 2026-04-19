package com.rafptor.parser.modca;

import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class EmbeddedObjectDataTest {

    @Test
    void detectsJpegPayload() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
        EmbeddedObjectData sf = EmbeddedObjectData.parse(new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xEE, 0x92), 0, jpeg));
        assertEquals(EmbeddedObjectData.Kind.JPEG, sf.kind());
        assertArrayEquals(jpeg, sf.payload());
    }

    @Test
    void detectsPngPayload() {
        byte[] png = new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        EmbeddedObjectData sf = EmbeddedObjectData.parse(new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xEE, 0x92), 0, png));
        assertEquals(EmbeddedObjectData.Kind.PNG, sf.kind());
    }

    @Test
    void unknownPayloadKindForNonImageBytes() {
        byte[] other = new byte[]{0x01, 0x02, 0x03, 0x04};
        EmbeddedObjectData sf = EmbeddedObjectData.parse(new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xEE, 0x92), 0, other));
        assertEquals(EmbeddedObjectData.Kind.UNKNOWN, sf.kind());
    }
}
