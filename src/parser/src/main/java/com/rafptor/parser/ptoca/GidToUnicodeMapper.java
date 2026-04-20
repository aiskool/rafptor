package com.rafptor.parser.ptoca;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a per-document {@code GID → Unicode} mapping for TrueType fonts
 * embedded via an MDR record.
 *
 * <p>When the font is encoded as Identity-H in the AFP stream the TRN bytes
 * are glyph ids (CIDs) rather than code points. The converter populates
 * this mapper from the font's cmap table at load time and the PTOCA
 * decoder consults it when the payload does not match any of its other
 * heuristics.
 *
 * <p>Thread-safety: internal {@link ConcurrentHashMap} — a single instance
 * can be shared across pages.
 */
public final class GidToUnicodeMapper {

    private final Map<Integer, Map<Integer, Integer>> perFontGidTables = new ConcurrentHashMap<>();

    public void registerFont(int localFontId, Map<Integer, Integer> gidToUnicode) {
        if (gidToUnicode == null) return;
        perFontGidTables.put(localFontId, Map.copyOf(gidToUnicode));
    }

    public boolean has(int localFontId) {
        return perFontGidTables.containsKey(localFontId);
    }

    /**
     * Translate a byte pair (GID hi, GID lo) to its Unicode code point.
     * Returns -1 if no mapping is known.
     */
    public int lookup(int localFontId, int gid) {
        Map<Integer, Integer> table = perFontGidTables.get(localFontId);
        if (table == null) return -1;
        Integer v = table.get(gid);
        return v == null ? -1 : v;
    }

    /** Decode a stream of 2-byte GIDs into a Unicode String. */
    public String decodeGids(int localFontId, byte[] data, int offset, int length) {
        if (!has(localFontId) || length <= 0) return "";
        StringBuilder sb = new StringBuilder(length / 2);
        for (int i = 0; i + 1 < length; i += 2) {
            int gid = ((data[offset + i] & 0xFF) << 8) | (data[offset + i + 1] & 0xFF);
            int cp = lookup(localFontId, gid);
            if (cp > 0) sb.appendCodePoint(cp);
        }
        return sb.toString();
    }
}
