package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FNI — Font Index ({@code D3 8C 89}).
 *
 * <p>Holds the per-character metrics of an IBM raster font: inline advance
 * ("character increment"), ascender/descender, and the offset of the glyph
 * bitmap inside the paired FNP (Font Patterns) block. Rafptor substitutes
 * Liberation / Noto glyphs for rendering, so the bitmap offsets are
 * unused; the character-increment value however is critical for
 * positioning downstream text correctly — without it, the renderer falls
 * back to Liberation metrics which rarely match IBM's.
 *
 * <p>Full wire format (spec FOCA AFPC-0006):
 *
 * <pre>
 *   [0..1]   number of character-increment entries
 *   [2..N]   repeating group of 8 bytes per character:
 *              [0..1]  GCID index
 *              [2..3]  character increment in font design units
 *              [4..5]  ascender
 *              [6..7]  descender
 * </pre>
 */
public record FontIndex(StructuredFieldId id, List<Entry> entries) implements AfpStructuredField {

    public record Entry(int gcidIndex, int increment, int ascender, int descender) { }

    public FontIndex {
        entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public static FontIndex parse(RawStructuredField raw) {
        byte[] data = raw.data();
        List<Entry> entries = new ArrayList<>();
        // Tolerate both header-less layouts (observed in certain AFPC streams)
        // and the spec-formal layout with a 2-byte count prefix.
        int start = 0;
        int count = -1;
        if (data.length >= 2) {
            count = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
            int predicted = 2 + count * 8;
            if (predicted <= data.length && count > 0) {
                start = 2;
            } else {
                count = -1;
            }
        }
        int end = count > 0 ? start + count * 8 : data.length - (data.length % 8);
        for (int i = start; i + 8 <= end; i += 8) {
            int gc   = ((data[i]     & 0xFF) << 8) | (data[i + 1] & 0xFF);
            int inc  = ((data[i + 2] & 0xFF) << 8) | (data[i + 3] & 0xFF);
            int asc  = ((data[i + 4] & 0xFF) << 8) | (data[i + 5] & 0xFF);
            int desc = ((data[i + 6] & 0xFF) << 8) | (data[i + 7] & 0xFF);
            entries.add(new Entry(gc, inc, asc, desc));
        }
        return new FontIndex(raw.id(), entries);
    }
}
