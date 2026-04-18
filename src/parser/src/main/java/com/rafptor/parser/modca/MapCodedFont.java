package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Map Coded Font (MCF) — maps a local font identifier (1-byte) to a coded-font
 * resource name referenced by downstream PTOCA control sequences.
 *
 * <p>Wire format is a sequence of Repeating Group entries. Rafptor parses the
 * "MCF-2" variant (one RG length byte followed by entries). Each entry declares
 * its length and a 1-byte local-id; the remaining bytes include the resource
 * name encoded in EBCDIC.
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

        // The MCF-2 variant starts every repeating-group with a length byte.
        // The older MCF-1 variant has a leading flag/reserved byte followed
        // by fixed-width 40-byte entries. If the first byte is not a plausible
        // MCF-2 length (>= 2 and fits the payload), fall back to MCF-1.
        int first = data[0] & 0xFF;
        boolean looksMcf2 = first >= 2 && first <= data.length;
        if (looksMcf2) {
            int pos = 0;
            while (pos < data.length) {
                int rgLength = data[pos] & 0xFF;
                if (rgLength < 2 || pos + rgLength > data.length) {
                    break;
                }
                int localId = data[pos + 1] & 0xFF;
                String name = ModcaUtil.decodeName(data, pos + 2, Math.min(8, rgLength - 2));
                entries.add(new Entry(localId, name));
                pos += rgLength;
            }
        } else {
            // MCF-1: one reserved byte, then repeating 40-byte entries.
            // Each entry: 1 local id, 1 reserved, 8 charset name, 8 code page, 22 reserved/metrics.
            int entrySize = 40;
            int pos = 1;
            while (pos + entrySize <= data.length) {
                int localId = data[pos] & 0xFF;
                String name = ModcaUtil.decodeName(data, pos + 2, 8);
                entries.add(new Entry(localId, name));
                pos += entrySize;
            }
        }
        return new MapCodedFont(raw.id(), Collections.unmodifiableList(entries));
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
