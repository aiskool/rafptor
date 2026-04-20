package com.rafptor.converter.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class IocaFs10DecoderTest {

    @Test
    void decodeRaw_produces_expected_dimensions() {
        // 16x8 pixels = 16 bytes (2 bytes per row × 8 rows).
        byte[] px = new byte[16];
        // Fill first row with 0xFF → inverted to 0x00 = fully white in AWT.
        px[0] = (byte) 0xFF; px[1] = (byte) 0xFF;
        BufferedImage img = IocaFs10Decoder.decodeRaw(px, 16, 8);
        assertNotNull(img);
        assertEquals(16, img.getWidth());
        assertEquals(8, img.getHeight());
    }

    @Test
    void decodeRaw_inverts_ink_convention() {
        byte[] px = {(byte) 0xFF};  // 8 pixels all-ink
        BufferedImage img = IocaFs10Decoder.decodeRaw(px, 8, 1);
        // RGB of (0,0): TYPE_BYTE_BINARY with bit 1 = ink → RGB 0x000000
        int rgb = img.getRGB(0, 0) & 0xFFFFFF;
        assertEquals(0x000000, rgb);
    }

    @Test
    void decodeRaw_rejects_undersized_buffer() {
        assertNull(IocaFs10Decoder.decodeRaw(new byte[]{0x00}, 16, 16));
        assertNull(IocaFs10Decoder.decodeRaw(null, 8, 8));
        assertNull(IocaFs10Decoder.decodeRaw(new byte[16], -1, 8));
    }

    @Test
    void decodeSegment_returns_null_on_empty_input() {
        assertNull(IocaFs10Decoder.decodeSegment(null));
        assertNull(IocaFs10Decoder.decodeSegment(new byte[5]));
    }
}
