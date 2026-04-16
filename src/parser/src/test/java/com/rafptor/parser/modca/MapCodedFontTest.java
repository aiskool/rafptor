package com.rafptor.parser.modca;

import com.rafptor.parser.AfpTestFileGenerator;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapCodedFontTest {

    @Test
    void parses_single_entry() {
        byte[] data = AfpTestFileGenerator.mcfPayload(1, "FONTBOLD");
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xAB, 0x8A), 0, data);
        MapCodedFont mcf = MapCodedFont.parse(raw);
        assertEquals(1, mcf.entries().size());
        assertEquals(1, mcf.entries().get(0).localId());
        assertEquals("FONTBOLD", mcf.entries().get(0).codedFontName());
    }

    @Test
    void parses_multiple_entries() {
        byte[] first = AfpTestFileGenerator.mcfPayload(1, "FONTA");
        byte[] second = AfpTestFileGenerator.mcfPayload(2, "FONTB");
        byte[] joined = new byte[first.length + second.length];
        System.arraycopy(first, 0, joined, 0, first.length);
        System.arraycopy(second, 0, joined, first.length, second.length);
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xAB, 0x8A), 0, joined);
        MapCodedFont mcf = MapCodedFont.parse(raw);
        assertEquals(2, mcf.entries().size());
        assertEquals(2, mcf.entries().get(1).localId());
    }
}
