package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * Presentation Text Descriptor (PTD) — structured field {@code 0xD3 0xB1 0x9B}.
 *
 * <p>Wire layout (object-area coordinate variant):
 * <pre>
 *   offset 0   XoaBase             1 byte
 *   offset 1   YoaBase             1 byte
 *   offset 2   XoaUnits            2 bytes  L-units per X base
 *   offset 4   YoaUnits            2 bytes
 *   offset 6   XoaSize             3 bytes
 *   offset 9   YoaSize             3 bytes
 * </pre>
 *
 * <p>Carries the L-units-per-inch used by PTOCA move inline / baseline control
 * sequences on the page. When absent the PGD resolution is used.
 */
public record PresentationTextDescriptor(
        StructuredFieldId id,
        int xUnitBase,
        int yUnitBase,
        int xUnitsPerBase,
        int yUnitsPerBase) implements AfpStructuredField {

    public static PresentationTextDescriptor parse(RawStructuredField raw) {
        byte[] data = raw.data();
        int xBase = safeByte(data, 0);
        int yBase = safeByte(data, 1);
        int xUnits = safeUShort(data, 2);
        int yUnits = safeUShort(data, 4);
        return new PresentationTextDescriptor(raw.id(), xBase, yBase, xUnits, yUnits);
    }

    public int xResolution() {
        return xUnitBase == 0x00 ? xUnitsPerBase / 10 : xUnitsPerBase;
    }

    public int yResolution() {
        return yUnitBase == 0x00 ? yUnitsPerBase / 10 : yUnitsPerBase;
    }

    private static int safeByte(byte[] d, int o) {
        return (o < d.length) ? d[o] & 0xFF : 0;
    }

    private static int safeUShort(byte[] d, int o) {
        if (o + 1 >= d.length) return 0;
        return ((d[o] & 0xFF) << 8) | (d[o + 1] & 0xFF);
    }
}
