package com.rafptor.parser.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructuredFieldIdTest {

    @Test
    void toHex_formats_bytes() {
        StructuredFieldId id = StructuredFieldId.of(0xD3, 0xA8, 0xA8);
        assertEquals("D3 A8 A8", id.toHex());
    }

    @Test
    void fromBytes_reads_three_bytes() {
        StructuredFieldId id = StructuredFieldId.fromBytes(new byte[]{(byte) 0xD3, (byte) 0xEE, (byte) 0x9B}, 0);
        assertEquals(0xD3, id.classByte());
        assertEquals(0xEE, id.typeByte());
        assertEquals(0x9B, id.categoryByte());
    }

    @Test
    void parseHex_round_trips() {
        StructuredFieldId id = StructuredFieldId.parseHex("D3A8A8");
        assertEquals(StructuredFieldId.of(0xD3, 0xA8, 0xA8), id);
    }

    @Test
    void rejects_out_of_range_bytes() {
        assertThrows(IllegalArgumentException.class, () -> StructuredFieldId.of(0x100, 0, 0));
    }
}
