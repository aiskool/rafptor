package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * Page Descriptor (PGD) — structured field {@code 0xD3 0xA6 0xAF}.
 *
 * <p>Wire layout (MO:DCA-P, the most common flavour):
 * <pre>
 *   offset 0   XpgBase          1 byte   unit base (0x00 = 10 inches)
 *   offset 1   YpgBase          1 byte
 *   offset 2   XpgUnits         2 bytes  L-units per XpgBase unit
 *   offset 4   YpgUnits         2 bytes
 *   offset 6   XpgSize          3 bytes  page width in L-units
 *   offset 9   YpgSize          3 bytes  page height in L-units
 *   offset 12  reserved         5 bytes
 * </pre>
 *
 * <p>Resolution (dpi) is derived from XpgUnits / YpgUnits when XpgBase/YpgBase
 * describe a one-inch unit; for the 0x00 (10-inches) base the L-units per inch
 * equal XpgUnits / 10.
 */
public record PageDescriptor(
        StructuredFieldId id,
        int xUnitBase,
        int yUnitBase,
        int xUnitsPerBase,
        int yUnitsPerBase,
        int widthLUnits,
        int heightLUnits) implements AfpStructuredField {

    public static PageDescriptor parse(RawStructuredField raw) {
        byte[] data = raw.data();
        int xBase = safeByte(data, 0);
        int yBase = safeByte(data, 1);
        int xUnits = safeUShort(data, 2);
        int yUnits = safeUShort(data, 4);
        int width = safe3Byte(data, 6);
        int height = safe3Byte(data, 9);
        return new PageDescriptor(raw.id(), xBase, yBase, xUnits, yUnits, width, height);
    }

    /** X resolution in L-units per inch. */
    public int xResolution() {
        return resolve(xUnitsPerBase, xUnitBase);
    }

    /** Y resolution in L-units per inch. */
    public int yResolution() {
        return resolve(yUnitsPerBase, yUnitBase);
    }

    private static int resolve(int unitsPerBase, int base) {
        // 0x00 → base of 10 inches; 0x01 → base of 10 cm; default to 1 inch.
        if (base == 0x00) {
            return unitsPerBase / 10;
        }
        return unitsPerBase > 0 ? unitsPerBase : 240;
    }

    private static int safeByte(byte[] d, int o) {
        return (o < d.length) ? d[o] & 0xFF : 0;
    }

    private static int safeUShort(byte[] d, int o) {
        if (o + 1 >= d.length) return 0;
        return ((d[o] & 0xFF) << 8) | (d[o + 1] & 0xFF);
    }

    private static int safe3Byte(byte[] d, int o) {
        if (o + 2 >= d.length) return 0;
        return ((d[o] & 0xFF) << 16) | ((d[o + 1] & 0xFF) << 8) | (d[o + 2] & 0xFF);
    }
}
