package com.rafptor.parser.ptoca;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Decodes Double-Byte Character Set (CJK) text runs.
 *
 * <p>In AFP mixed-mode text, shift-out (0x0E) switches from SBCS to DBCS and
 * shift-in (0x0F) switches back. Each DBCS character is two bytes. When the
 * MCF declares a pure DBCS coded font (no shift bytes), {@link #decodePure}
 * decodes every even-byte pair directly.
 *
 * <p>Supported DBCS charsets rely on the JDK's built-in IBM-930/933/935/937
 * converters, which perform the SO/SI mode switching natively — we only
 * need to feed them the raw bytes.
 */
public final class DbcsDecoder {

    private final Charset dbcsCharset;

    public DbcsDecoder(String jvmCharsetName) {
        this.dbcsCharset = Charset.isSupported(jvmCharsetName)
                ? Charset.forName(jvmCharsetName)
                : StandardCharsets.UTF_8;
    }

    /** Decode a pure-DBCS run (every 2 bytes = 1 character, no SO/SI). */
    public String decodePure(byte[] data, int offset, int length) {
        if (data == null || length <= 0) return "";
        int safeLen = length - (length % 2);
        if (safeLen <= 0) return "";
        return new String(data, offset, safeLen, dbcsCharset);
    }

    /**
     * Decode mixed SBCS/DBCS text where 0x0E/0x0F mode-switch characters are
     * embedded in the run. JDK IBM-930 / IBM-939 handle this natively — we
     * simply delegate.
     */
    public String decodeMixed(byte[] data, int offset, int length) {
        if (data == null || length <= 0) return "";
        return new String(data, offset, length, dbcsCharset);
    }
}
