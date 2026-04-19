package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import com.rafptor.parser.reader.TripletParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Map Data Resource (MDR) — structured field {@code 0xD3 0xAB 0xC3}.
 *
 * <p>MO:DCA/P5 composers (Doc1, Adobe Output, Compart, and the AFPWorld
 * sample) use MDR to bind local font identifiers to external TrueType or
 * Unicode font resources that the PTOCA stream subsequently selects via
 * SCFL. Each repeating group describes one font and carries:
 * <ul>
 *   <li>a 2-byte RG length header,</li>
 *   <li>a triplet {@code 0x10} "Resource Local Identifier",</li>
 *   <li>a triplet {@code 0x8B} font size (bytes 4–5 = size in 1/20 pt),</li>
 *   <li>a triplet {@code 0x02} "Fully Qualified Name" carrying the font
 *       name as UTF-16LE starting at offset 4,</li>
 *   <li>a final triplet {@code 0x02} with subtype {@code 0xBE} whose last
 *       byte is the local font id that PTOCA's SCFL will reference.</li>
 * </ul>
 *
 * <p>The plain triplet list is still exposed for backward compatibility
 * with legacy MDR-as-flat-triplets producers.
 */
public record MapDataResource(StructuredFieldId id,
                              List<TripletParser.Triplet> triplets,
                              List<FontEntry> fontEntries) implements AfpStructuredField {

    public MapDataResource {
        triplets = List.copyOf(triplets);
        fontEntries = List.copyOf(fontEntries);
    }

    public static MapDataResource parse(RawStructuredField raw) {
        byte[] data = raw.data();
        List<TripletParser.Triplet> triplets = TripletParser.parseAll(data, 0, data.length);
        List<FontEntry> fonts = parseFontEntries(data);
        return new MapDataResource(raw.id(), triplets, fonts);
    }

    /**
     * Walk the MDR payload as a sequence of 2-byte-prefixed repeating groups
     * and harvest one {@link FontEntry} per RG. Malformed groups are skipped
     * rather than aborting the parse.
     */
    private static List<FontEntry> parseFontEntries(byte[] data) {
        List<FontEntry> out = new ArrayList<>();
        int p = 0;
        while (p + 2 <= data.length) {
            int rgLen = ((data[p] & 0xFF) << 8) | (data[p + 1] & 0xFF);
            if (rgLen < 4 || p + rgLen > data.length) {
                break;
            }
            FontEntry entry = parseOneRg(data, p + 2, p + rgLen);
            if (entry != null) {
                out.add(entry);
            }
            p += rgLen;
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Parse triplets inside one MDR repeating group and build a FontEntry.
     * We care about three triplets:
     * <ul>
     *   <li>{@code 0x8B} — size in 1/20 pt (bytes 2..3 after triplet header).</li>
     *   <li>{@code 0x02 ... 0xDE 0x00 0x00} — UTF-16LE font name.</li>
     *   <li>{@code 0x02 ... 0xBE 0x00 <id>} — local font id (trailing byte).</li>
     * </ul>
     */
    private static FontEntry parseOneRg(byte[] data, int start, int end) {
        String name = "";
        double pointSize = 0;
        int localId = -1;
        int p = start;
        while (p + 2 <= end) {
            int tl = data[p] & 0xFF;
            if (tl < 2 || p + tl > end) break;
            int tid = data[p + 1] & 0xFF;
            if (tid == 0x8B && tl >= 6) {
                int sizeRaw = ((data[p + 4] & 0xFF) << 8) | (data[p + 5] & 0xFF);
                if (sizeRaw > 0) {
                    pointSize = sizeRaw / 20.0;
                }
            } else if (tid == 0x02 && tl >= 5) {
                int subtype = data[p + 2] & 0xFF;
                if (subtype == 0xDE && tl >= 6) {
                    // Triplet 0x02 / subtype 0xDE wire layout:
                    //   [tl][id=0x02][0xDE][format=0x00]  <UTF-16BE name>
                    // Name bytes occupy positions 4..tl-1. MO:DCA/P5 producers
                    // emit the name as UTF-16 *big-endian* even though
                    // Windows tools historically store font names in LE.
                    // Strip any U+0000 padding before returning.
                    int nameStart = p + 4;
                    int nameLen = tl - 4;
                    if ((nameLen & 1) == 1) nameLen -= 1;
                    if (nameLen >= 2 && name.isEmpty()) {
                        name = decodeUtf16Be(data, nameStart, nameLen)
                                .replace("\u0000", "")
                                .trim();
                    }
                } else if (subtype == 0xBE) {
                    // Last byte carries the local font id.
                    localId = data[p + tl - 1] & 0xFF;
                }
            }
            p += tl;
        }
        if (name.isEmpty() && localId < 0) {
            return null;
        }
        return new FontEntry(Math.max(localId, 0), name, pointSize);
    }

    private static String decodeUtf16Be(byte[] data, int offset, int length) {
        try {
            return new String(data, offset, length, StandardCharsets.UTF_16BE);
        } catch (RuntimeException e) {
            return "";
        }
    }

    /**
     * Bound local-font-id → resource font entry.
     *
     * @param localId     SCFL local font id referencing this entry.
     * @param fontName    human-readable font name (e.g. "Arial Bold"), or empty.
     * @param pointSize   declared point size, or 0 if the MDR did not carry one.
     */
    public record FontEntry(int localId, String fontName, double pointSize) {
        public FontEntry {
            if (fontName == null) fontName = "";
            if (pointSize < 0) pointSize = 0;
        }
    }
}
