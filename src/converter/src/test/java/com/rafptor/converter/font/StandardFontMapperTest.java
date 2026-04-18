package com.rafptor.converter.font;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardFontMapperTest {

    @Test
    void loadsDefaultRegistry() {
        List<FontMapping> mappings = FontMappingRegistry.loadDefault();
        assertNotNull(mappings);
        // The comprehensive catalog ships more than the legacy four entries.
        assertTrue(mappings.size() >= 4, "expected at least 4 mappings, got " + mappings.size());
    }

    @Test
    void mapsCourierExactMatch() {
        StandardFontMapper m = new StandardFontMapper();
        FontMapping map = m.map("T1V10500", "C0H200");
        assertEquals("Liberation Mono", map.trueTypeFont());
        assertEquals("IBM500", map.ebcdicEncoding());
    }

    @Test
    void mapsSerifExactMatch() {
        StandardFontMapper m = new StandardFontMapper();
        FontMapping map = m.map("T1V10500", "C0S200");
        assertEquals("Liberation Serif", map.trueTypeFont());
    }

    @Test
    void fallsBackToDefaultMappingOnMiss() {
        StandardFontMapper m = new StandardFontMapper();
        FontMapping map = m.map("UNKNOWN", "UNKNOWN");
        assertEquals("Liberation Mono", map.trueTypeFont());
    }

    @Test
    void frenchCodePageMapping() {
        StandardFontMapper m = new StandardFontMapper();
        FontMapping map = m.map("T1GI1147", "C0H200");
        assertEquals("IBM1147", map.ebcdicEncoding());
    }
}
