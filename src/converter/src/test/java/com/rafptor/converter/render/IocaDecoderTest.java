package com.rafptor.converter.render;

import com.rafptor.parser.model.AfpImageObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class IocaDecoderTest {

    @Test
    void returnsNullOnEmptyImage() {
        AfpImageObject img = new AfpImageObject(
                "empty", AfpImageObject.Encoding.IOCA_FS45, 0, 0, new byte[0]);
        assertNull(IocaDecoder.decode(img));
    }

    @Test
    void decodesUncompressed1bppRaster() throws Exception {
        // 4×4 image, 1 bit per pixel, MSB first: all 1s = all ink = all black.
        int w = 4, h = 4;
        byte rasterByte = (byte) 0xFF; // 1111 1111 — covers 8 pixels, we only use 4 per row
        byte[] raster = new byte[h];
        for (int y = 0; y < h; y++) raster[y] = rasterByte;

        byte[] payload = buildIocaPayload(w, h, 1, /* compression */ 0x01, raster);
        AfpImageObject img = new AfpImageObject(
                "u1bpp", AfpImageObject.Encoding.IOCA_FS45, 0, 0, payload);
        IocaDecoder.Decoded decoded = IocaDecoder.decode(img);
        assertNotNull(decoded);
        assertEquals(w, decoded.widthPx());
        assertEquals(h, decoded.heightPx());
        // Top-left pixel: 1 bit = ink → black (0,0,0)
        int rgb = decoded.image().getRGB(0, 0) & 0xFFFFFF;
        assertEquals(0x000000, rgb);
    }

    @Test
    void returnsNullOnUnsupportedCompression() {
        byte[] payload = buildIocaPayload(2, 2, 1, /* exotic */ 0x7F, new byte[]{0, 0});
        AfpImageObject img = new AfpImageObject(
                "weird", AfpImageObject.Encoding.IOCA_FS45, 0, 0, payload);
        assertNull(IocaDecoder.decode(img));
    }

    /**
     * Minimal IOCA payload using the real self-describing field layout:
     * short fields are {@code [id 1][len 1][body …]}; the Image Data field
     * {@code 0xFE} is the long form {@code [0xFE][sub-type 1][len 2 BE][body]}.
     */
    private static byte[] buildIocaPayload(int w, int h, int bpp, int compression, byte[] raster) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Begin Segment (no body)
            writeShort(out, 0x70, new byte[0]);
            // Begin Image Content (marker body 0xFF = no explicit ID)
            writeShort(out, 0x91, new byte[]{(byte) 0xFF});
            // Image Size — 9-byte form: unit, hReso(2), vReso(2), hSize(2), vSize(2)
            byte[] size = new byte[]{
                    0x00,
                    0x00, 0x78,        // h-reso 120
                    0x00, 0x78,        // v-reso 120
                    (byte) (w >> 8), (byte) (w & 0xFF),
                    (byte) (h >> 8), (byte) (h & 0xFF)
            };
            writeShort(out, 0x94, size);
            // Image Encoding — compression, recording, bit order
            writeShort(out, 0x95, new byte[]{(byte) compression, 0x03, 0x01});
            // IDE size — bpp
            writeShort(out, 0x96, new byte[]{(byte) bpp});
            // Image Data (long form)
            writeData(out, raster);
            // End Image Content
            writeShort(out, 0x93, new byte[0]);
            // End Segment
            writeShort(out, 0x71, new byte[0]);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeShort(ByteArrayOutputStream out, int id, byte[] body) {
        out.write(id);
        out.write(body.length & 0xFF);
        out.write(body, 0, body.length);
    }

    private static void writeData(ByteArrayOutputStream out, byte[] body) {
        out.write(0xFE);
        out.write(0x92);                             // sub-type: Image Data
        out.write((body.length >> 8) & 0xFF);
        out.write(body.length & 0xFF);
        out.write(body, 0, body.length);
    }
}
