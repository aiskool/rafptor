package com.rafptor.parser.modca;

import java.nio.charset.Charset;

final class ModcaUtil {

    /** EBCDIC code page used for AFP resource/name fields (NAME parameter). */
    static final Charset NAME_CHARSET = Charset.forName("IBM500");

    private ModcaUtil() {
    }

    /**
     * Decodes the 1..8-byte name parameter common to BDT, BPG, BAG, BRG, BOG and MCF rows.
     * Returns an empty string if the data is empty.
     * AFP name fields are EBCDIC-encoded and space-padded; trailing spaces are trimmed.
     */
    static String decodeName(byte[] data, int offset, int length) {
        if (length <= 0) {
            return "";
        }
        if (offset < 0 || offset + length > data.length) {
            return "";
        }
        String raw = new String(data, offset, length, NAME_CHARSET);
        return raw.stripTrailing();
    }

    static String decodeFirstName(byte[] data, int maxLength) {
        int len = Math.min(data.length, maxLength);
        return decodeName(data, 0, len);
    }
}
