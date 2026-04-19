package com.rafptor.parser.ptoca;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AfpCodePageMapperTest {

    @Test
    void resolves_known_t1v10_names() {
        assertEquals("IBM037", AfpCodePageMapper.resolve("T1V10037"));
        assertEquals("IBM273", AfpCodePageMapper.resolve("T1V10273"));
        assertEquals("IBM500", AfpCodePageMapper.resolve("T1V10500"));
        assertEquals("IBM297", AfpCodePageMapper.resolve("T1V10297"));
    }

    @Test
    void resolves_euro_t1v01_names() {
        assertEquals("IBM1140", AfpCodePageMapper.resolve("T1V01140"));
        assertEquals("IBM1141", AfpCodePageMapper.resolve("T1V01141"));
        assertEquals("IBM1147", AfpCodePageMapper.resolve("T1V01147"));
        assertEquals("IBM1148", AfpCodePageMapper.resolve("T1V01148"));
    }

    @Test
    void resolves_generic_aliases() {
        assertEquals("IBM500", AfpCodePageMapper.resolve("T1D0BASE"));
    }

    @Test
    void is_case_insensitive_and_trims() {
        assertEquals("IBM037", AfpCodePageMapper.resolve("  t1v10037  "));
    }

    @Test
    void unknown_names_fall_back_to_ibm500() {
        assertEquals("IBM500", AfpCodePageMapper.resolve(""));
        assertEquals("IBM500", AfpCodePageMapper.resolve(null));
        assertEquals("IBM500", AfpCodePageMapper.resolve("T1BOGUS9"));
        assertEquals("IBM500", AfpCodePageMapper.resolve("NONSENSE"));
    }

    @Test
    void numeric_suffix_probing_when_jvm_supports_it() {
        // IBM277 (Norwegian) is a standard JDK EBCDIC charset; even without an
        // explicit entry the suffix probe must still land on it.
        assertEquals("IBM277", AfpCodePageMapper.resolve("T1V10277"));
    }

    @Test
    void resolve_charset_returns_non_null_jvm_charset() {
        Charset cs = AfpCodePageMapper.resolveCharset("T1V10500");
        assertNotNull(cs);
        assertEquals("IBM500", cs.name());
    }
}
