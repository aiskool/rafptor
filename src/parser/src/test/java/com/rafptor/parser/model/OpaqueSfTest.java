package com.rafptor.parser.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpaqueSfTest {

    @Test
    void body_is_cloned_and_truncated_to_max_bytes() {
        byte[] big = new byte[OpaqueSf.MAX_BODY_BYTES + 1_000];
        OpaqueSf sf = new OpaqueSf(42L, "aabbcc", 0, big);
        assertEquals(OpaqueSf.MAX_BODY_BYTES + 1_000, sf.originalLength());
        assertEquals(OpaqueSf.MAX_BODY_BYTES, sf.body().length);
        assertTrue(sf.truncated());
    }

    @Test
    void idHex_is_normalised_to_upper_case() {
        OpaqueSf sf = new OpaqueSf(0, "d3a8a8", 0, new byte[0]);
        assertEquals("D3A8A8", sf.idHex());
    }

    @Test
    void human_readable_id_uses_registry() {
        OpaqueSf sf = new OpaqueSf(0, "D3EE9B", 0, new byte[0]);
        assertEquals("PTX", sf.humanReadableId());
        OpaqueSf unknown = new OpaqueSf(0, "FFEEDD", 0, new byte[0]);
        assertEquals("UNKNOWN", unknown.humanReadableId());
    }
}
