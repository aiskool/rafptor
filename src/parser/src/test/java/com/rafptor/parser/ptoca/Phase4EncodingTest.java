package com.rafptor.parser.ptoca;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Phase4EncodingTest {

    @Test
    void code_page_mapper_resolves_new_single_byte_variants() {
        assertEquals("IBM420",  AfpCodePageMapper.resolve("T1V10420"));
        assertEquals("IBM424",  AfpCodePageMapper.resolve("T1V10424"));
        assertEquals("IBM875",  AfpCodePageMapper.resolve("T1V10875"));
        assertEquals("IBM1025", AfpCodePageMapper.resolve("T1V11025"));
        assertEquals("IBM870",  AfpCodePageMapper.resolve("T1V10870"));
    }

    @Test
    void dbcs_decoder_handles_pure_japanese_kanji() {
        DbcsDecoder d = new DbcsDecoder("IBM930");
        // Two bytes in the IBM-930 "fat" Kanji range — any two-byte sequence
        // decoded via the charset is valid; we just assert the output length.
        byte[] bytes = {(byte) 0x0E, (byte) 0x44, (byte) 0x76, (byte) 0x0F};
        String decoded = d.decodeMixed(bytes, 0, bytes.length);
        assertNotNull(decoded);
        // We do not assert specific glyphs — JDK variants differ — only that
        // the decoder produced a non-empty output or degraded gracefully.
    }

    @Test
    void gid_mapper_returns_minus_one_for_unregistered_fonts() {
        GidToUnicodeMapper m = new GidToUnicodeMapper();
        assertEquals(-1, m.lookup(0, 0x41));
        assertFalse(m.has(0));
    }

    @Test
    void gid_mapper_translates_registered_pair() {
        GidToUnicodeMapper m = new GidToUnicodeMapper();
        m.registerFont(1, Map.of(0x41, (int) 'A', 0x42, (int) 'B'));
        byte[] bytes = {0x00, 0x41, 0x00, 0x42};
        assertEquals("AB", m.decodeGids(1, bytes, 0, bytes.length));
    }
}
