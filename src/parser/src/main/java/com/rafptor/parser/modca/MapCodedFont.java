package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Map Coded Font (MCF) — binds a local font identifier (1 byte) to a coded-font
 * resource name referenced by downstream PTOCA control sequences.
 *
 * <p>Three wire-format variants are accepted:
 * <ul>
 *   <li><b>MCF-2 with leading flag</b>: one flag byte followed by repeating
 *       groups ({@code [RG length][local id][reserved][rotation][reserved][triplets]…}).
 *       Each triplet has {@code [length][id][value…]}; triplet id {@code 0x86} carries
 *       the character-set resource name, {@code 0x85} carries the code-page name, and
 *       {@code 0x02} is the Fully-Qualified Name used by some producers.</li>
 *   <li><b>MCF-2 no-flag</b>: repeating groups starting immediately. Each RG
 *       begins with its length byte.</li>
 *   <li><b>MCF-1</b>: one reserved byte then fixed 40-byte entries whose bytes
 *       2..9 hold the character-set name.</li>
 * </ul>
 *
 * <p>Unknown or malformed bytes are skipped rather than throwing.
 */
public record MapCodedFont(StructuredFieldId id, List<Entry> entries) implements AfpStructuredField {

    public MapCodedFont {
        entries = List.copyOf(entries);
    }

    public static MapCodedFont parse(RawStructuredField raw) {
        byte[] data = raw.data();
        List<Entry> entries = new ArrayList<>();
        if (data.length == 0) {
            return new MapCodedFont(raw.id(), Collections.unmodifiableList(entries));
        }

        int first = data[0] & 0xFF;
        // The banking-grade variant used by Exstream, Doc1 and others has:
        //   [flag-byte] [00 LL local-id reserved rot reserved] triplets…
        // where LL is a single byte >= 6 and the RG starts with 0x00 filler.
        if (data.length > 6 && (data[1] & 0xFF) == 0x00
                && (data[2] & 0xFF) >= 6 && (data[2] & 0xFF) <= 0x7F) {
            parseRepeatingGroupsTwoByte(data, 1, entries);
        } else if (first == 0x00 && data.length > 1 && (data[1] & 0xFF) >= 2) {
            // MCF-2 with leading flag byte (single-byte length).
            parseRepeatingGroups(data, 1, entries);
        } else if (first >= 2 && first <= data.length) {
            // Classic MCF-2: RGs starting at offset 0, plain 8-byte-name form.
            parseFlatMcf2(data, entries);
        } else {
            // MCF-1 fallback: reserved byte then fixed 40-byte entries.
            parseMcf1(data, entries);
        }
        return new MapCodedFont(raw.id(), Collections.unmodifiableList(entries));
    }

    private static void parseRepeatingGroups(byte[] data, int start, List<Entry> entries) {
        int pos = start;
        while (pos + 2 <= data.length) {
            int rgLength = data[pos] & 0xFF;
            if (rgLength < 2 || pos + rgLength > data.length) break;
            int localId = data[pos + 1] & 0xFF;
            // Walk triplets inside this RG; look for a name-carrying triplet.
            String charsetName = "";
            String codepageName = "";
            int tripletStart = pos + 4; // skip localId(1) + reserved(1) + rotation(1) + reserved(1)
            int tripletEnd = pos + rgLength;
            int tPos = tripletStart;
            while (tPos + 2 <= tripletEnd) {
                int tLen = data[tPos] & 0xFF;
                if (tLen < 2 || tPos + tLen > tripletEnd) break;
                int tId = data[tPos + 1] & 0xFF;
                if (tLen > 4 && (tId == 0x86 || tId == 0x02)) {
                    // 0x86 = Resource Local Identifier (character-set name)
                    // 0x02 = Fully Qualified Name (class-dependent, often charset)
                    int nameLen = Math.min(8, tLen - 4);
                    int nameOffset = tPos + 4;
                    if (nameOffset + nameLen <= tripletEnd) {
                        String candidate = ModcaUtil.decodeName(data, nameOffset, nameLen);
                        if (charsetName.isEmpty() && looksLikeCharset(candidate)) {
                            charsetName = candidate;
                        }
                    }
                } else if (tLen > 4 && tId == 0x85) {
                    // Code-page name — we expose it via the name fallback but
                    // prefer the character-set name for mapping purposes.
                    int nameLen = Math.min(8, tLen - 4);
                    int nameOffset = tPos + 4;
                    if (nameOffset + nameLen <= tripletEnd) {
                        codepageName = ModcaUtil.decodeName(data, nameOffset, nameLen);
                    }
                }
                tPos += tLen;
            }
            String name = !charsetName.isEmpty() ? charsetName : codepageName;
            if (name.isEmpty()) {
                name = ModcaUtil.decodeName(data, pos + 2, Math.min(8, rgLength - 2));
            }
            entries.add(new Entry(localId, name));
            pos += rgLength;
        }
    }

    private static void parseFlatMcf2(byte[] data, List<Entry> entries) {
        int pos = 0;
        while (pos < data.length) {
            int rgLength = data[pos] & 0xFF;
            if (rgLength < 2 || pos + rgLength > data.length) break;
            int localId = data[pos + 1] & 0xFF;
            String name = ModcaUtil.decodeName(data, pos + 2, Math.min(8, rgLength - 2));
            entries.add(new Entry(localId, name));
            pos += rgLength;
        }
    }

    private static void parseMcf1(byte[] data, List<Entry> entries) {
        int entrySize = 40;
        int pos = 1;
        while (pos + entrySize <= data.length) {
            int localId = data[pos] & 0xFF;
            String name = ModcaUtil.decodeName(data, pos + 2, 8);
            entries.add(new Entry(localId, name));
            pos += entrySize;
        }
    }

    /**
     * Banking-grade MCF-2 RG variant: one leading 0x00 byte, then a 1-byte
     * length covering the RG. The name is inside triplets 0x86 / 0x02 / 0x85.
     */
    /**
     * Banking-grade MCF-2 RG variant used by Exstream, Doc1 and related
     * producers. Wire layout per repeating group:
     * <pre>
     *   [0] 0x00           reserved
     *   [1] LL             RG length (covers bytes 0..LL-1)
     *   [2] ?              reserved (0x04 typically)
     *   [3] ?              section/rotation byte (0x24 typically)
     *   [4] ?              reserved (0x05 typically)
     *   [5] local font id
     *   [6..LL]            triplets (0x02 FQN etc.)
     * </pre>
     */
    private static void parseRepeatingGroupsTwoByte(byte[] data, int start, List<Entry> entries) {
        int pos = start;
        while (pos + 6 <= data.length) {
            if ((data[pos] & 0xFF) != 0x00) break;
            int rgLength = data[pos + 1] & 0xFF;
            if (rgLength < 6 || pos + rgLength > data.length) break;
            int localId = data[pos + 5] & 0xFF;
            String name = extractNameFromTriplets(data, pos + 6, pos + rgLength);
            if (name.isEmpty()) {
                name = ModcaUtil.decodeName(data, pos + 6,
                        Math.max(0, Math.min(8, rgLength - 6)));
            }
            entries.add(new Entry(localId, name));
            pos += rgLength;
        }
    }

    /**
     * Walk the triplets inside a repeating group and prefer the character-set
     * name. Triplet X'02' (Fully Qualified Name) carries a sub-type at byte+2
     * where X'86' = Character Set, X'85' = Coded Font, X'84' = Code Page.
     */
    private static String extractNameFromTriplets(byte[] data, int from, int to) {
        String charset = "";
        String codedFont = "";
        String codepage = "";
        int p = from;
        while (p + 2 <= to) {
            int tLen = data[p] & 0xFF;
            if (tLen < 2 || p + tLen > to) break;
            int tId = data[p + 1] & 0xFF;
            if (tLen >= 12 && tId == 0x02) {
                int fqnType = data[p + 2] & 0xFF;
                String name = ModcaUtil.decodeName(data, p + 4, 8);
                switch (fqnType) {
                    case 0x86 -> { if (charset.isEmpty()) charset = name; }
                    case 0x85 -> { if (codedFont.isEmpty()) codedFont = name; }
                    case 0x84 -> { if (codepage.isEmpty()) codepage = name; }
                    default -> { /* unknown FQN subtype */ }
                }
            } else if (tLen >= 12 && tId == 0x86 && charset.isEmpty()) {
                // Some producers emit the Resource Local Identifier triplet
                // directly (less common).
                String candidate = ModcaUtil.decodeName(data, p + 4, 8);
                if (looksLikeCharset(candidate)) charset = candidate;
            }
            p += tLen;
        }
        // Prefer character-set name (starts with C0…) since it encodes the
        // typographic family we can map. Fall back to coded-font then code-page.
        if (!charset.isEmpty()) return charset;
        if (!codedFont.isEmpty()) return codedFont;
        return codepage;
    }

    private static boolean looksLikeCharset(String s) {
        // IBM coded-font / character-set resource names start with 'C0', 'X0',
        // 'T1' or similar two-character prefix followed by alphanumerics.
        if (s == null || s.length() < 4) return false;
        char a = s.charAt(0);
        char b = s.charAt(1);
        return (a == 'C' || a == 'X' || a == 'T') && Character.isLetterOrDigit(b);
    }

    public record Entry(int localId, String codedFontName) {
        public Entry {
            if ((localId & ~0xFF) != 0) {
                throw new IllegalArgumentException("localId must be a byte");
            }
            if (codedFontName == null) {
                codedFontName = "";
            }
        }
    }
}
