package com.rafptor.converter.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class BcocaDecoderTest {

    @Test
    void parse_extracts_symbology_and_payload() {
        byte[] bdd = {
                0x00, 0x01,             // version
                0x17,                   // type = Code 128
                0x00,                   // modifier
                0x00, 0x02,             // module width
                0x00, 0x28              // height = 40
        };
        byte[] bda = "12345678".getBytes();
        BcocaDecoder.BarcodeRequest req = BcocaDecoder.parse(bdd, bda);
        assertNotNull(req);
        assertEquals(BcocaDecoder.Symbology.CODE_128, req.symbology());
        assertEquals("12345678", req.payload());
        assertEquals(2, req.moduleWidthPx());
        assertEquals(40, req.heightPx());
    }

    @Test
    void render_code128_via_zxing() {
        BufferedImage img = BcocaDecoder.renderSymbol(
                BcocaDecoder.Symbology.CODE_128, "HELLO", 200, 50);
        assertNotNull(img, "ZXing should encode Code 128 for ASCII payload");
        assertEquals(200, img.getWidth());
        assertEquals(50, img.getHeight());
    }

    @Test
    void render_qr_code_via_zxing() {
        BufferedImage img = BcocaDecoder.renderSymbol(
                BcocaDecoder.Symbology.QR_CODE, "https://rafptor.example", 128, 128);
        assertNotNull(img);
        assertEquals(128, img.getWidth());
    }

    @Test
    void render_returns_null_for_unknown_symbology() {
        assertNull(BcocaDecoder.renderSymbol(
                BcocaDecoder.Symbology.UNKNOWN, "x", 100, 20));
        assertNull(BcocaDecoder.renderSymbol(
                BcocaDecoder.Symbology.USPS_4STATE, "x", 100, 20));
    }

    @Test
    void parse_rejects_too_short_bdd() {
        assertNull(BcocaDecoder.parse(new byte[]{0x00}, "x".getBytes()));
        assertNull(BcocaDecoder.parse(null, null));
    }
}
