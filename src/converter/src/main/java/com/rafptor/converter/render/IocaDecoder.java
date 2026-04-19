package com.rafptor.converter.render;

import com.rafptor.parser.model.AfpImageObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Best-effort IOCA Function-Set 45 decoder.
 *
 * <p>IOCA images arrive as a sequence of self-describing fields inside the
 * {@code BIM/EIM} envelope. Each field is
 * {@code [field-id 1 byte][length 1 byte][payload]} (IOCA long-form with a
 * 2-byte length prefixed by {@code 0xFE} is also supported for the Image
 * Data field). The fields we consume:
 * <ul>
 *   <li>{@code 0x70} Begin Segment.</li>
 *   <li>{@code 0x71} End Segment.</li>
 *   <li>{@code 0x91} Begin Image Content.</li>
 *   <li>{@code 0x93} End Image Content.</li>
 *   <li>{@code 0x94} Image Size parameter — variant with
 *       {@code unit-base(1) h-reso(2) v-reso(2) h-size(2) v-size(2)}.</li>
 *   <li>{@code 0x95} Image Encoding — {@code [compression-id 1][recording 1][bit-order 1]}.
 *       {@code compression-id} 0x03 = Group 4 (T.6), 0x0B = JBIG2, 0x01 = uncompressed.</li>
 *   <li>{@code 0x96} Image IDE Size — bits per pixel (1, 8, 24).</li>
 *   <li>{@code 0xFE} Image Data — long-form segment with a 2-byte length.
 *       The raster bytes of one or more consecutive data segments concatenate.</li>
 * </ul>
 *
 * <p>Routing:
 * <ul>
 *   <li>Uncompressed (compression 0x01 or 0x00) → direct raster build.</li>
 *   <li>G4 (0x03) → wrap into a minimal Group-4 TIFF in memory and let
 *       TwelveMonkeys' TIFF reader decode it.</li>
 *   <li>JBIG2 (0x0B) → let PDFBox's jbig2-imageio handle it.</li>
 *   <li>Anything else → {@code null}, caller falls back to a warning.</li>
 * </ul>
 *
 * <p>Self-describing fields that do not affect the raster content (colour
 * table, IDE structure, image resolution) are intentionally ignored.
 */
public final class IocaDecoder {

    private IocaDecoder() {}

    /** Result carrier so callers can skip {@code null} checks at every step. */
    public record Decoded(BufferedImage image, int widthPx, int heightPx) {}

    /**
     * Decode an AFP image object. Returns {@code null} when the encoding is
     * unsupported or the raster cannot be made sense of.
     */
    public static Decoded decode(AfpImageObject img) {
        if (img == null || img.byteCount() == 0) return null;
        byte[] raw = img.rawBytes();
        Segments s = parseSegments(raw);
        if (s.dataBytes.length == 0) return null;
        int compression = s.compression;
        int width = s.widthPx;
        int height = s.heightPx;
        int bitsPerPixel = s.bitsPerPixel > 0 ? s.bitsPerPixel : 1;

        try {
            switch (compression) {
                case 0x00:
                case 0x01: // uncompressed
                    return buildUncompressed(s.dataBytes, width, height, bitsPerPixel);
                case 0x03: // Group 4 (CCITT T.6)
                    return decodeG4(s.dataBytes, width, height);
                case 0x0B: // JBIG2
                    return decodeJbig2(s.dataBytes);
                default:
                    // Fall through to ImageIO sniff — some producers embed a
                    // plain JPEG inside IOCA as compression 0x83.
                    return sniffWithImageIO(raw);
            }
        } catch (Exception e) {
            return null;
        }
    }

    // ── segment walk ─────────────────────────────────────────────────────

    static final class Segments {
        int widthPx;
        int heightPx;
        int bitsPerPixel;
        int compression = -1;
        byte[] dataBytes = new byte[0];
    }

    static Segments parseSegments(byte[] raw) {
        Segments s = new Segments();
        ByteArrayOutputStream dataAcc = new ByteArrayOutputStream();
        int pos = 0;
        while (pos + 2 <= raw.length) {
            int segId = raw[pos] & 0xFF;
            int length;
            int payloadStart;
            // 0xFE "Image Data" is the long form: id(1) ext(1) len(2 BE) payload
            // The data byte right after 0xFE is a sub-type/extension (often 0x92
            // "IDE" or 0x9C "Image Data Element"). The 2 bytes after that are
            // the big-endian length.
            if (segId == 0xFE) {
                if (pos + 4 > raw.length) break;
                length = ((raw[pos + 2] & 0xFF) << 8) | (raw[pos + 3] & 0xFF);
                payloadStart = pos + 4;
            } else {
                length = raw[pos + 1] & 0xFF;
                payloadStart = pos + 2;
            }
            int payloadEnd = payloadStart + length;
            if (payloadEnd > raw.length) break;

            switch (segId) {
                case 0x70, 0x71, 0x91, 0x93 -> {
                    // Segment/content envelope markers — body is metadata we
                    // don't need to interpret.
                }
                case 0x94 -> readImageSize(s, raw, payloadStart, length);
                case 0x95 -> {
                    if (length >= 1) s.compression = raw[payloadStart] & 0xFF;
                }
                case 0x96 -> {
                    if (length >= 1) s.bitsPerPixel = raw[payloadStart] & 0xFF;
                }
                case 0xFE -> dataAcc.write(raw, payloadStart, length);
                default -> { /* ignore unknown fields */ }
            }
            pos = payloadEnd;
        }
        s.dataBytes = dataAcc.toByteArray();
        return s;
    }

    private static void readImageSize(Segments s, byte[] raw, int off, int len) {
        // unit-base(1) hReso(2) vReso(2) hSize(2) vSize(2) = 9 bytes
        if (len >= 9) {
            int hSize = ((raw[off + 5] & 0xFF) << 8) | (raw[off + 6] & 0xFF);
            int vSize = ((raw[off + 7] & 0xFF) << 8) | (raw[off + 8] & 0xFF);
            if (s.widthPx == 0) s.widthPx = hSize;
            if (s.heightPx == 0) s.heightPx = vSize;
        }
    }

    // ── uncompressed ─────────────────────────────────────────────────────

    private static Decoded buildUncompressed(byte[] data, int w, int h, int bpp) {
        if (w <= 0 || h <= 0) return null;
        if (bpp != 1 && bpp != 8 && bpp != 24) return null;
        try {
            if (bpp == 1) {
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
                int rowBytes = (w + 7) / 8;
                WritableRaster raster = img.getRaster();
                int[] samples = new int[w];
                for (int y = 0; y < h; y++) {
                    int rowOff = y * rowBytes;
                    if (rowOff + rowBytes > data.length) break;
                    for (int x = 0; x < w; x++) {
                        int b = data[rowOff + (x >> 3)] & 0xFF;
                        int bit = (b >> (7 - (x & 7))) & 0x01;
                        // AFP convention: 1 = ink → black → 0 in BYTE_BINARY.
                        samples[x] = 1 - bit;
                    }
                    raster.setPixels(0, y, w, 1, samples);
                }
                return new Decoded(img, w, h);
            }
            if (bpp == 8) {
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
                int rowBytes = w;
                int[] samples = new int[w];
                for (int y = 0; y < h; y++) {
                    int rowOff = y * rowBytes;
                    if (rowOff + rowBytes > data.length) break;
                    for (int x = 0; x < w; x++) samples[x] = data[rowOff + x] & 0xFF;
                    img.getRaster().setPixels(0, y, w, 1, samples);
                }
                return new Decoded(img, w, h);
            }
            // bpp == 24 RGB
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            int rowBytes = w * 3;
            for (int y = 0; y < h; y++) {
                int rowOff = y * rowBytes;
                if (rowOff + rowBytes > data.length) break;
                for (int x = 0; x < w; x++) {
                    int r = data[rowOff + 3 * x] & 0xFF;
                    int g = data[rowOff + 3 * x + 1] & 0xFF;
                    int b = data[rowOff + 3 * x + 2] & 0xFF;
                    img.setRGB(x, y, (r << 16) | (g << 8) | b);
                }
            }
            return new Decoded(img, w, h);
        } catch (Exception e) {
            return null;
        }
    }

    // ── G4 via TIFF container ────────────────────────────────────────────

    /**
     * Wrap the G4 raster bytes into a minimal single-strip TIFF and let
     * TwelveMonkeys decode it. We build only the mandatory baseline tags and
     * the Group4 compression marker.
     */
    private static Decoded decodeG4(byte[] data, int w, int h) throws IOException {
        if (w <= 0 || h <= 0) return null;
        byte[] tiff = buildMinimalG4Tiff(data, w, h);
        try (ByteArrayInputStream in = new ByteArrayInputStream(tiff)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) return null;
            return new Decoded(img, img.getWidth(), img.getHeight());
        }
    }

    private static byte[] buildMinimalG4Tiff(byte[] data, int w, int h) throws IOException {
        // TIFF II (little-endian) with 9 IFD entries, single strip.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeByte('I'); dos.writeByte('I');  // little-endian
        writeLE16(dos, 42);                      // TIFF magic
        writeLE32(dos, 8);                       // offset to first IFD
        int entries = 9;
        writeLE16(dos, entries);
        int dataOffset = 8 + 2 + entries * 12 + 4;
        writeIfdEntry(dos, 0x0100, 3, 1, w);                         // ImageWidth
        writeIfdEntry(dos, 0x0101, 3, 1, h);                         // ImageLength
        writeIfdEntry(dos, 0x0102, 3, 1, 1);                         // BitsPerSample
        writeIfdEntry(dos, 0x0103, 3, 1, 4);                         // Compression = T.6 (Group 4)
        writeIfdEntry(dos, 0x0106, 3, 1, 0);                         // PhotometricInterpretation = WhiteIsZero
        writeIfdEntry(dos, 0x0111, 4, 1, dataOffset);                // StripOffsets
        writeIfdEntry(dos, 0x0116, 3, 1, h);                         // RowsPerStrip
        writeIfdEntry(dos, 0x0117, 4, 1, data.length);               // StripByteCounts
        writeIfdEntry(dos, 0x011A, 5, 1, 0);                         // XResolution (placeholder 0/1)
        writeLE32(dos, 0);                                           // next IFD offset
        dos.write(data);
        return out.toByteArray();
    }

    private static void writeIfdEntry(DataOutputStream dos, int tag, int type, int count, int value) throws IOException {
        writeLE16(dos, tag);
        writeLE16(dos, type);
        writeLE32(dos, count);
        writeLE32(dos, value);
    }

    private static void writeLE16(DataOutputStream dos, int v) throws IOException {
        dos.writeByte(v & 0xFF);
        dos.writeByte((v >> 8) & 0xFF);
    }

    private static void writeLE32(DataOutputStream dos, int v) throws IOException {
        dos.writeByte(v & 0xFF);
        dos.writeByte((v >> 8) & 0xFF);
        dos.writeByte((v >> 16) & 0xFF);
        dos.writeByte((v >> 24) & 0xFF);
    }

    // ── JBIG2 via PDFBox jbig2-imageio ───────────────────────────────────

    private static Decoded decodeJbig2(byte[] data) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) return null;
            return new Decoded(img, img.getWidth(), img.getHeight());
        }
    }

    // ── generic ImageIO sniff (JPEG, PNG, GIF, BMP …) ────────────────────

    private static Decoded sniffWithImageIO(byte[] data) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) return null;
            return new Decoded(img, img.getWidth(), img.getHeight());
        }
    }
}
