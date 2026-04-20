package com.rafptor.converter.render;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;

/**
 * IOCA Function-Set 10 decoder.
 *
 * <p>FS10 is the simplest IOCA flavour: 1-bit-per-pixel uncompressed bitmap
 * stored row-major, MSB-first within each byte, rows padded to a whole
 * number of bytes. Pixel value 1 = ink (black), 0 = paper (white).
 *
 * <p>The raw image-segment stream as observed in MO:DCA uses a handful of
 * self-describing parameter triplets (ISP=Image Size Parameter 0x94, IDE Size
 * 0x96, …) wrapping the raw pixel bytes. This decoder accepts either
 * (a) a pre-unwrapped pixel buffer plus width/height or (b) a full IOCA
 * segment stream, falling back to a safe "could not decode" marker.
 */
public final class IocaFs10Decoder {

    private IocaFs10Decoder() {
    }

    /** Decode a raw, row-major, MSB-first 1-bit bitmap into a TYPE_BYTE_BINARY image. */
    public static BufferedImage decodeRaw(byte[] pixels, int width, int height) {
        if (pixels == null || width <= 0 || height <= 0) {
            return null;
        }
        int rowBytes = (width + 7) / 8;
        if (pixels.length < rowBytes * height) {
            return null;
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        byte[] dst = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        // In TYPE_BYTE_BINARY the data buffer is exactly (w+7)/8 * h bytes,
        // same layout as FS10 — but the AWT convention is 0=ink, 1=paper,
        // opposite of FS10. Invert while copying.
        int srcRowBytes = rowBytes;
        int dstRowBytes = (width + 7) / 8;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < dstRowBytes; x++) {
                int srcByte = pixels[y * srcRowBytes + x] & 0xFF;
                dst[y * dstRowBytes + x] = (byte) (~srcByte & 0xFF);
            }
        }
        return image;
    }

    /**
     * Attempt to locate the ISP / pixel-data pair in a captured IOCA image
     * segment and return the decoded BufferedImage, or {@code null} if the
     * segment is too exotic. Never throws — an unexpected byte sequence
     * yields null so the caller can fall back to the placeholder path.
     */
    public static BufferedImage decodeSegment(byte[] segment) {
        if (segment == null || segment.length < 10) return null;
        int i = 0;
        int width = -1, height = -1, ideSize = -1;
        byte[] pixels = null;
        while (i + 1 < segment.length) {
            int id = segment[i] & 0xFF;
            int len = segment[i + 1] & 0xFF;
            if (len == 0 || i + 2 + len > segment.length) break;
            int p = i + 2;
            switch (id) {
                case 0x94 -> { // ISP Image Size Parameter
                    if (len >= 9) {
                        // 1 byte unit / 2 bytes h-res / 2 bytes v-res / 2 bytes h-size / 2 bytes v-size
                        width  = ((segment[p + 5] & 0xFF) << 8) | (segment[p + 6] & 0xFF);
                        height = ((segment[p + 7] & 0xFF) << 8) | (segment[p + 8] & 0xFF);
                    }
                }
                case 0x96 -> { // IDE Size
                    if (len >= 1) ideSize = segment[p] & 0xFF;
                }
                case 0xFE -> { // Image Data
                    pixels = Arrays.copyOfRange(segment, p, p + len);
                }
                default -> { /* skip */ }
            }
            i = p + len;
        }
        if (pixels == null || width <= 0 || height <= 0 || ideSize != 1) {
            return null;
        }
        return decodeRaw(pixels, width, height);
    }
}
