package com.rafptor.parser;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Builds minimal AFP byte streams for unit tests.
 *
 * <p>Every record follows the canonical layout: {@code 0x5A LL LL ID1 ID2 ID3 FLAGS RES RES DATA*}.
 */
public final class AfpTestFileGenerator {

    public static final int CC = 0x5A;
    public static final Charset EBCDIC = Charset.forName("IBM500");

    private AfpTestFileGenerator() {
    }

    public static byte[] createMinimalDocument() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeRecord(out, 0xD3, 0xA8, 0xA8, namePayload("DOC00001"));
        writeRecord(out, 0xD3, 0xA8, 0xAF, namePayload("PAGE0001"));
        writeRecord(out, 0xD3, 0xA8, 0xC9, namePayload("AEG00001"));
        writeRecord(out, 0xD3, 0xAB, 0x8A, mcfPayload(1, "FONTBOLD"));
        writeRecord(out, 0xD3, 0xA9, 0xC9, namePayload("AEG00001"));
        writeRecord(out, 0xD3, 0xEE, 0x9B, ptocaPayload("HELLO"));
        writeRecord(out, 0xD3, 0xA9, 0xAF, namePayload("PAGE0001"));
        writeRecord(out, 0xD3, 0xA9, 0xA8, namePayload("DOC00001"));
        return out.toByteArray();
    }

    public static byte[] createMultipageDocument(int pages) {
        if (pages <= 0) {
            throw new IllegalArgumentException("pages must be > 0");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeRecord(out, 0xD3, 0xA8, 0xA8, namePayload("MULTIDOC"));
        for (int i = 0; i < pages; i++) {
            String pname = String.format("PAGE%04d", i + 1);
            writeRecord(out, 0xD3, 0xA8, 0xAF, namePayload(pname));
            writeRecord(out, 0xD3, 0xEE, 0x9B, ptocaPayload("P" + (i + 1)));
            writeRecord(out, 0xD3, 0xA9, 0xAF, namePayload(pname));
        }
        writeRecord(out, 0xD3, 0xA9, 0xA8, namePayload("MULTIDOC"));
        return out.toByteArray();
    }

    public static byte[] createDocumentWithTle(Map<String, String> tags) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeRecord(out, 0xD3, 0xA8, 0xA8, namePayload("TLEDOC"));
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            writeRecord(out, 0xD3, 0xA0, 0x90, tlePayload(entry.getKey(), entry.getValue()));
        }
        writeRecord(out, 0xD3, 0xA8, 0xAF, namePayload("PAGE0001"));
        writeRecord(out, 0xD3, 0xA9, 0xAF, namePayload("PAGE0001"));
        writeRecord(out, 0xD3, 0xA9, 0xA8, namePayload("TLEDOC"));
        return out.toByteArray();
    }

    public static byte[] createOversizedField(int declaredLength) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(CC);
        out.write((declaredLength >>> 8) & 0xFF);
        out.write(declaredLength & 0xFF);
        out.write(0xD3);
        out.write(0xEE);
        out.write(0xEE);
        out.write(0x00);
        out.write(0x00);
        out.write(0x00);
        return out.toByteArray();
    }

    public static byte[] createTruncatedStream() {
        byte[] ok = createMinimalDocument();
        byte[] truncated = new byte[ok.length - 5];
        System.arraycopy(ok, 0, truncated, 0, truncated.length);
        return truncated;
    }

    public static byte[] createStreamMissingCarriageControl() {
        return new byte[]{0x00, 0x00, 0x08, (byte) 0xD3, (byte) 0xA8, (byte) 0xA8, 0x00, 0x00};
    }

    // ---- payload helpers ----

    public static byte[] namePayload(String name) {
        String padded = (name + "        ").substring(0, 8);
        return padded.getBytes(EBCDIC);
    }

    public static byte[] mcfPayload(int localId, String fontName) {
        String padded = (fontName + "        ").substring(0, 8);
        byte[] ebc = padded.getBytes(EBCDIC);
        byte[] out = new byte[2 + ebc.length];
        out[0] = (byte) (2 + ebc.length);
        out[1] = (byte) (localId & 0xFF);
        System.arraycopy(ebc, 0, out, 2, ebc.length);
        return out;
    }

    public static byte[] tlePayload(String key, String value) {
        byte[] keyBytes = key.getBytes(EBCDIC);
        byte[] valueBytes = value.getBytes(EBCDIC);
        byte[] tripletValue = new byte[2 + valueBytes.length];
        tripletValue[0] = (byte) (2 + valueBytes.length);
        tripletValue[1] = 0x02;
        System.arraycopy(valueBytes, 0, tripletValue, 2, valueBytes.length);

        byte[] tripletKey = new byte[2 + keyBytes.length];
        tripletKey[0] = (byte) (2 + keyBytes.length);
        tripletKey[1] = 0x36;
        System.arraycopy(keyBytes, 0, tripletKey, 2, keyBytes.length);

        byte[] out = new byte[tripletKey.length + tripletValue.length];
        System.arraycopy(tripletKey, 0, out, 0, tripletKey.length);
        System.arraycopy(tripletValue, 0, out, tripletKey.length, tripletValue.length);
        return out;
    }

    /** Produce a PTOCA payload: SCFL(1), AMB(0), AMI(0), TRN(text). */
    public static byte[] ptocaPayload(String text) {
        byte[] ebc = text.getBytes(EBCDIC);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // chained-form sequences: LL FN ...
        write(out, (byte) 0x03, (byte) 0xF1, (byte) 0x01); // SCFL local id = 1
        write(out, (byte) 0x04, (byte) 0xD2, (byte) 0x00, (byte) 0x00); // AMB 0
        write(out, (byte) 0x04, (byte) 0xC6, (byte) 0x00, (byte) 0x00); // AMI 0
        out.write((byte) (2 + ebc.length));
        out.write((byte) 0xDA);
        out.writeBytes(ebc);
        return out.toByteArray();
    }

    // ---- low level writing ----

    public static void writeRecord(ByteArrayOutputStream out, int id1, int id2, int id3, byte[] data) {
        int total = 8 + data.length;
        if (total > 0xFFFF) {
            throw new IllegalStateException("record > 65535 bytes");
        }
        out.write(CC);
        out.write((total >>> 8) & 0xFF);
        out.write(total & 0xFF);
        out.write(id1);
        out.write(id2);
        out.write(id3);
        out.write(0x00); // flags
        out.write(0x00);
        out.write(0x00);
        out.writeBytes(data);
    }

    private static void write(ByteArrayOutputStream out, byte... bytes) {
        for (byte b : bytes) {
            out.write(b);
        }
    }
}
