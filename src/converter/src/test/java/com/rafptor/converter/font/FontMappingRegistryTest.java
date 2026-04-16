package com.rafptor.converter.font;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontMappingRegistryTest {

    @Test
    void parsesMinimalDocument() {
        String json = "{\"mappings\":[{" +
                "\"afp_codepage\":\"A\"," +
                "\"afp_charset_prefix\":\"B\"," +
                "\"ebcdic_encoding\":\"IBM500\"," +
                "\"truetype_font\":\"Liberation Mono\"," +
                "\"fallback_font\":\"Courier\"," +
                "\"scale_factor\":1.25," +
                "\"baseline_offset\":0.5," +
                "\"default_point_size\":12" +
                "}]}";
        List<FontMapping> parsed = FontMappingRegistry.parse(json);
        assertEquals(1, parsed.size());
        FontMapping m = parsed.get(0);
        assertEquals("A", m.afpCodePage());
        assertEquals("B", m.afpCharsetPrefix());
        assertEquals(1.25, m.scaleFactor(), 1e-9);
        assertEquals(12.0, m.defaultPointSize(), 1e-9);
    }

    @Test
    void returnsEmptyOnMissingMappings() {
        assertTrue(FontMappingRegistry.parse("{}").isEmpty());
    }
}
