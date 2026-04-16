package com.rafptor.parser.modca;

import com.rafptor.parser.exception.MalformedFieldException;
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
        int pos = 0;
        while (pos < data.length) {
            int rgLength = data[pos] & 0xFF;
            if (rgLength < 2) {
                throw new MalformedFieldException("MCF entry length " + rgLength + " < 2 at offset " + pos);
            }
            if (pos + rgLength > data.length) {
                throw new MalformedFieldException(
                        "MCF entry of declared length " + rgLength + " extends past data at offset " + pos);
            }
            int localId = data[pos + 1] & 0xFF;
            String name = ModcaUtil.decodeName(data, pos + 2, Math.min(8, rgLength - 2));
            entries.add(new Entry(localId, name));
            pos += rgLength;
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
