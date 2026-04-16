package com.rafptor.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserLimitsTest {

    @Test
    void defaults_are_set() {
        ParserLimits limits = ParserLimits.defaults();
        assertEquals(ParserLimits.DEFAULT_MAX_FIELD_SIZE, limits.maxFieldSize());
        assertEquals(ParserLimits.DEFAULT_MAX_DOCUMENT_SIZE, limits.maxDocumentSize());
        assertEquals(ParserLimits.DEFAULT_MAX_NESTING_DEPTH, limits.maxNestingDepth());
        assertEquals(ParserLimits.DEFAULT_PARSE_TIMEOUT_MILLIS, limits.parseTimeoutMillis());
    }

    @Test
    void rejects_non_positive_values() {
        assertThrows(IllegalArgumentException.class, () -> new ParserLimits(0, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ParserLimits(1, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ParserLimits(1, 1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ParserLimits(1, 1, 1, 0));
    }
}
