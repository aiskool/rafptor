package com.rafptor.converter.render;

import org.apache.commons.imaging.Imaging;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * IOCA Function-Set 11 decoder — CCITT Group 3 / Group 4 compressed bitmaps.
 *
 * <p>FS11 is the fax-compression flavour: ~10× smaller than FS10 on
 * scanned text, ubiquitous in banking statements and insurance forms.
 * Apache Commons Imaging (Apache 2.0) carries a native Modified-READ
 * (T.6 / Group 4) decoder we can feed with a TIFF-wrapped payload. The
 * trick: Group-4 data inside AFP is usually raw encoded stripes, so we
 * wrap them in a minimal TIFF header on the fly and let commons-imaging
 * decode.
 *
 * <p>If the bytes cannot be decoded (missing header, unsupported
 * modifier), returns {@code null} so the caller emits a placeholder.
 */
public final class IocaFs11Decoder {

    private IocaFs11Decoder() {
    }

    /**
     * Attempt to decode a CCITT Group-4-compressed AFP stripe wrapped in a
     * minimal synthetic TIFF. This works for the common case produced by
     * IBM Infoprint / Xerox / Ricoh where the compressor emits the raw
     * G4 bitstream with no TIFF strip wrapper.
     */
    public static BufferedImage decodeG4(byte[] compressed, int width, int height) {
        if (compressed == null || width <= 0 || height <= 0) return null;
        byte[] tiff = wrapAsTiff(compressed, width, height, 4);
        return decodeTiff(tiff);
    }

    /** Same as {@link #decodeG4} for CCITT Group 3 (T.4). */
    public static BufferedImage decodeG3(byte[] compressed, int width, int height) {
        if (compressed == null || width <= 0 || height <= 0) return null;
        byte[] tiff = wrapAsTiff(compressed, width, height, 3);
        return decodeTiff(tiff);
    }

    private static BufferedImage decodeTiff(byte[] tiff) {
        try {
            return Imaging.getBufferedImage(tiff);
        } catch (IOException e) {
            return null;
        } catch (RuntimeException e) {
            // commons-imaging 1.0.0-alpha5 wraps some format errors in
            // runtime exceptions (e.g. when the synthetic TIFF does not
            // represent a decodable strip). Treat them as "unable to decode".
            return null;
        }
    }

    /**
     * Produce a minimal single-strip TIFF wrapping a CCITT-encoded payload.
     * Using big-endian byte order; only the tags commons-imaging actually
     * reads are emitted.
     */
    private static byte[] wrapAsTiff(byte[] payload, int width, int height, int ccittGroup) {
        // 12 tag entries, each 12 bytes + 2-byte count + 4-byte nextIFD + 8-byte header
        int numTags = 10;
        int headerSize = 8;
        int ifdSize = 2 + numTags * 12 + 4;
        int dataOffset = headerSize + ifdSize;
        byte[] out = new byte[dataOffset + payload.length];
        int pos = 0;
        // TIFF header (big-endian 'MM', magic 42, IFD offset = headerSize)
        out[pos++] = 'M'; out[pos++] = 'M';
        writeU16(out, pos, 42); pos += 2;
        writeU32(out, pos, headerSize); pos += 4;
        // IFD: tag count
        writeU16(out, pos, numTags); pos += 2;
        // Tag entries (id, type=3 short / 4 long, count=1, value)
        pos = writeTagU32(out, pos, 0x0100, width);           // ImageWidth
        pos = writeTagU32(out, pos, 0x0101, height);          // ImageLength
        pos = writeTagU16(out, pos, 0x0102, 1);               // BitsPerSample=1
        pos = writeTagU16(out, pos, 0x0103, ccittGroup == 4 ? 4 : 3); // Compression
        pos = writeTagU16(out, pos, 0x0106, 0);               // PhotometricInterpretation=WhiteIsZero
        pos = writeTagU32(out, pos, 0x0111, dataOffset);      // StripOffsets
        pos = writeTagU16(out, pos, 0x0115, 1);               // SamplesPerPixel=1
        pos = writeTagU32(out, pos, 0x0116, height);          // RowsPerStrip=height
        pos = writeTagU32(out, pos, 0x0117, payload.length);  // StripByteCounts
        pos = writeTagU16(out, pos, 0x011C, 1);               // PlanarConfiguration=chunky
        // IFD end: next IFD offset = 0
        writeU32(out, pos, 0); pos += 4;
        System.arraycopy(payload, 0, out, dataOffset, payload.length);
        return out;
    }

    private static int writeTagU16(byte[] b, int p, int id, int value) {
        writeU16(b, p, id);
        writeU16(b, p + 2, 3);       // type=SHORT
        writeU32(b, p + 4, 1);       // count
        writeU16(b, p + 8, value);   // 2 bytes value, left-aligned
        writeU16(b, p + 10, 0);
        return p + 12;
    }

    private static int writeTagU32(byte[] b, int p, int id, int value) {
        writeU16(b, p, id);
        writeU16(b, p + 2, 4);       // type=LONG
        writeU32(b, p + 4, 1);       // count
        writeU32(b, p + 8, value);   // 4 bytes value
        return p + 12;
    }

    private static void writeU16(byte[] b, int p, int v) {
        b[p]     = (byte) ((v >> 8) & 0xFF);
        b[p + 1] = (byte) (v & 0xFF);
    }

    private static void writeU32(byte[] b, int p, int v) {
        b[p]     = (byte) ((v >> 24) & 0xFF);
        b[p + 1] = (byte) ((v >> 16) & 0xFF);
        b[p + 2] = (byte) ((v >> 8) & 0xFF);
        b[p + 3] = (byte) (v & 0xFF);
    }
}
