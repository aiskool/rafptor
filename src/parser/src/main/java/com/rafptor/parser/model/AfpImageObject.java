package com.rafptor.parser.model;

/**
 * An IOCA / IM image object collected from a {@code BIM/EIM} structured-field
 * envelope. {@code rawBytes} concatenates every {@code IRD} (Image Raster Data)
 * payload seen between {@code BIM} and {@code EIM}, in order. The encoding
 * identifier (e.g. {@code "JPEG"}, {@code "IOCA_FS45"}) is derived from the
 * {@code IDD} when available or left as {@link Encoding#UNKNOWN}.
 */
public final class AfpImageObject {

    public enum Encoding {
        JPEG,
        BMP,
        G3_MMR,
        IOCA_FS45,
        UNKNOWN,
    }

    private final String name;
    private final Encoding encoding;
    private final int widthLUnits;
    private final int heightLUnits;
    private final byte[] rawBytes;

    public AfpImageObject(String name, Encoding encoding, int widthLUnits, int heightLUnits, byte[] rawBytes) {
        if (encoding == null) {
            throw new IllegalArgumentException("encoding must not be null");
        }
        if (rawBytes == null) {
            throw new IllegalArgumentException("rawBytes must not be null");
        }
        this.name = name == null ? "" : name;
        this.encoding = encoding;
        this.widthLUnits = widthLUnits;
        this.heightLUnits = heightLUnits;
        this.rawBytes = rawBytes.clone();
    }

    public String name() { return name; }
    public Encoding encoding() { return encoding; }
    public int widthLUnits() { return widthLUnits; }
    public int heightLUnits() { return heightLUnits; }

    public byte[] rawBytes() { return rawBytes.clone(); }

    public int byteCount() { return rawBytes.length; }
}
