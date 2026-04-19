package com.rafptor.parser.modca;

import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BeginNamedResourceTest {

    @Test
    void extractsEbcdicResourceName() {
        // EBCDIC "I0000001" = C9 F0 F0 F0 F0 F0 F0 F1, then padding
        byte[] data = new byte[]{(byte) 0xC9, (byte) 0xF0, (byte) 0xF0, (byte) 0xF0,
                (byte) 0xF0, (byte) 0xF0, (byte) 0xF0, (byte) 0xF1, 0x00, 0x00};
        BeginNamedResource sf = BeginNamedResource.parse(new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xA8, 0xCE), 0, data));
        assertEquals("I0000001", sf.resourceName());
    }

    @Test
    void toleratesShortPayload() {
        BeginNamedResource sf = BeginNamedResource.parse(new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xA8, 0xA5), 0, new byte[]{0}));
        assertEquals("", sf.resourceName());
    }
}
