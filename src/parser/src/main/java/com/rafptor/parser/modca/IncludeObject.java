package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * Include Object (IOB) — structured field {@code 0xD3 0xAF 0xC3}.
 *
 * <p>Places a named object resource on the page. Wire layout used by most
 * MO:DCA/P5 composers (AFPWorld, DOC1):
 * <pre>
 *   offset 0..7    ObjectName      8 bytes EBCDIC resource name
 *   offset 8       ObjectType      1 byte
 *   offset 9       reserved        1 byte
 *   offset 10..12  XoaOset         3 bytes inline origin in L-units
 *   offset 13..15  YoaOset         3 bytes baseline origin in L-units
 *   offset 16..17  XoaOrent        2 bytes orientation (I-axis)
 *   offset 18..19  YoaOrent        2 bytes orientation (B-axis)
 *   offset 20..22  XocaOset        3 bytes referenced object X size
 *   offset 23..25  YocaOset        3 bytes referenced object Y size
 *   offset 26..    triplets
 * </pre>
 *
 * <p>Origin + size are expressed in the object environment's L-units
 * (typically 1440/inch). Callers convert to PDF points via the page
 * geometry.
 */
public record IncludeObject(StructuredFieldId id, String objectName,
                            int xOriginLUnits, int yOriginLUnits,
                            int xSizeLUnits, int ySizeLUnits)
        implements AfpStructuredField {

    public static IncludeObject parse(RawStructuredField raw) {
        byte[] d = raw.data();
        String name = ModcaUtil.decodeFirstName(d, 8);
        int xo = read3(d, 10);
        int yo = read3(d, 13);
        // MO:DCA IOB carries the object-area size as a triplet X'4C'
        // somewhere after the fixed header rather than at a fixed offset.
        // Byte 20..22 was the pre-triplet convention; keep it as a fallback.
        int xs = read3(d, 20);
        int ys = read3(d, 23);
        int[] size = scanObjectAreaSizeTriplet(d);
        if (size != null) {
            xs = size[0];
            ys = size[1];
        }
        return new IncludeObject(raw.id(), name, xo, yo, xs, ys);
    }

    /**
     * Walk the tail of the IOB body looking for triplet X'4C' (OBJECT AREA
     * SIZE). Its wire layout: {@code [tl=0x09][tid=0x4C][base][xExt3][yExt3]}.
     * Returns {@code {xExtent, yExtent}} in L-units or {@code null} if the
     * triplet is absent or malformed.
     */
    private static int[] scanObjectAreaSizeTriplet(byte[] d) {
        // Triplet search starts after the fixed header. The standard header
        // is 26 bytes, but several producers add 10 bytes of qualifier data;
        // we scan from the earliest plausible position (16) to the end.
        for (int start = 16; start + 9 <= d.length; start++) {
            int tl = d[start] & 0xFF;
            int tid = d[start + 1] & 0xFF;
            if (tl == 0x09 && tid == 0x4C) {
                int xExt = read3(d, start + 3);
                int yExt = read3(d, start + 6);
                if (xExt > 0 && yExt > 0) {
                    return new int[]{xExt, yExt};
                }
            }
        }
        return null;
    }

    private static int read3(byte[] d, int o) {
        if (o + 2 >= d.length) return 0;
        return ((d[o] & 0xFF) << 16) | ((d[o + 1] & 0xFF) << 8) | (d[o + 2] & 0xFF);
    }
}
