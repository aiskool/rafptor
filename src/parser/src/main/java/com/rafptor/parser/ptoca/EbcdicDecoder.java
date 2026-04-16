package com.rafptor.parser.ptoca;

import java.nio.charset.Charset;

/**
 * Thin wrapper around the JDK-native EBCDIC code pages.
 *
 * <p>Typical AFP code pages:
 * <ul>
 *   <li>IBM500 — International No. 5</li>
 *   <li>IBM1047 — Open Systems Latin-1</li>
 *   <li>IBM1147 — France (€)</li>
 * </ul>
 */
public final class EbcdicDecoder {

    private final Charset charset;

    public EbcdicDecoder(String charsetName) {
        if (charsetName == null || charsetName.isBlank()) {
            throw new IllegalArgumentException("charsetName must not be blank");
        }
        this.charset = Charset.forName(charsetName);
    }

    public static EbcdicDecoder ibm500() {
        return new EbcdicDecoder("IBM500");
    }

    public Charset charset() {
        return charset;
    }

    public String decode(byte[] data, int offset, int length) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (length <= 0) {
            return "";
        }
        if (offset < 0 || offset + length > data.length) {
            throw new IndexOutOfBoundsException("offset/length out of range");
        }
        return new String(data, offset, length, charset);
    }

    public String decodeAll(byte[] data) {
        return decode(data, 0, data.length);
    }
}
