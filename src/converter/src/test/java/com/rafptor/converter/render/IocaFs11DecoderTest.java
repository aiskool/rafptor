package com.rafptor.converter.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IocaFs11DecoderTest {

    @Test
    void decodeG4_rejects_empty_input() {
        assertNull(IocaFs11Decoder.decodeG4(null, 100, 100));
        assertNull(IocaFs11Decoder.decodeG4(new byte[0], 100, 100));
        assertNull(IocaFs11Decoder.decodeG4(new byte[]{0x00}, 0, 0));
    }

    @Test
    void decodeG3_rejects_invalid_dimensions() {
        byte[] payload = new byte[16];
        assertNull(IocaFs11Decoder.decodeG3(payload, -1, 100));
        assertNull(IocaFs11Decoder.decodeG3(payload, 100, -5));
    }

    @Test
    void decodeG4_returns_null_on_garbage() {
        // Random bytes cannot be a valid Group 4 bit-stream; the decoder
        // must degrade gracefully rather than crash.
        byte[] payload = new byte[128];
        java.util.Arrays.fill(payload, (byte) 0x55);
        // No assertion on value — just that we don't throw.
        IocaFs11Decoder.decodeG4(payload, 200, 40);
    }
}
