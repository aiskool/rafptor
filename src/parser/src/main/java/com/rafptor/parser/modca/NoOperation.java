package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * No Operation (NOP) — opaque commentary or vendor metadata.
 *
 * <p>Rafptor preserves the length plus, when the payload is plausibly text
 * (EBCDIC IBM500 or ASCII), a best-effort decoded form kept under the
 * hood for the audit trail. The decoded text is <b>never</b> logged and
 * never exposed through {@link #toString()} — the caller must request it
 * explicitly via {@link #decodedMessage()}.
 */
public record NoOperation(StructuredFieldId id, int payloadLength, String decodedMessage)
        implements AfpStructuredField {

    public NoOperation {
        if (decodedMessage == null) decodedMessage = "";
    }

    public static NoOperation parse(RawStructuredField raw) {
        byte[] data = raw.data();
        String decoded = tryDecode(data);
        return new NoOperation(raw.id(), raw.dataLength(), decoded);
    }

    private static String tryDecode(byte[] data) {
        if (data == null || data.length == 0) return "";
        String ebcdic = decodeOrEmpty(data, Charset.forName("IBM500"));
        if (looksPrintable(ebcdic)) return ebcdic;
        String ascii = decodeOrEmpty(data, StandardCharsets.US_ASCII);
        if (looksPrintable(ascii)) return ascii;
        return "";
    }

    private static String decodeOrEmpty(byte[] data, Charset cs) {
        try {
            return new String(data, cs);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean looksPrintable(String s) {
        if (s == null || s.isEmpty()) return false;
        int printable = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t' || (c >= 0x20 && c < 0x7F)) {
                printable++;
            }
        }
        return printable * 4 >= s.length() * 3;
    }

    @Override
    public String toString() {
        return "NoOperation[id=" + id + ", payloadLength=" + payloadLength + "]";
    }
}
