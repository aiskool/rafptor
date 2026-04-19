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
        int xs = read3(d, 20);
        int ys = read3(d, 23);
        return new IncludeObject(raw.id(), name, xo, yo, xs, ys);
    }

    private static int read3(byte[] d, int o) {
        if (o + 2 >= d.length) return 0;
        return ((d[o] & 0xFF) << 16) | ((d[o + 1] & 0xFF) << 8) | (d[o + 2] & 0xFF);
    }
}
