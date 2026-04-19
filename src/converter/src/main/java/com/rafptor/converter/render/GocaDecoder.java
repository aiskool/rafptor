package com.rafptor.converter.render;

import com.rafptor.parser.model.AfpGraphicObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal GOCA Drawing-Order walker. Supports the two families of orders
 * that cover almost all banking-statement graphics:
 *
 * <ul>
 *   <li><b>GLINE</b> ({@code 0x81} 2-byte, {@code 0xC1} 4-byte) — straight
 *       line from the current position to an absolute {@code (x,y)} point.
 *       The optional chained form accepts additional point pairs.</li>
 *   <li><b>GBOX</b> ({@code 0xC0}) — axis-aligned rectangle from the current
 *       position to {@code (x,y)}.</li>
 *   <li><b>GSLW</b> ({@code 0x19}) — set line width in design units.</li>
 *   <li><b>GSCOL</b> ({@code 0x0A}) — set stroke colour by index (subset of
 *       the GOCA standard palette).</li>
 *   <li><b>GSPS</b> ({@code 0x71}) — set current point, no drawing.</li>
 * </ul>
 *
 * <p>Unknown orders are skipped using their declared length — we never guess
 * past the next order boundary.
 *
 * <p>Coordinates produced by this decoder are in the GOCA drawing-order
 * integer space (usually signed 16-bit big-endian). The
 * {@link com.rafptor.converter.transform.GraphicTransformer} normalises them
 * to PDF points using the enclosing page resolution.
 */
public final class GocaDecoder {

    private GocaDecoder() {}

    /** A primitive the renderer must emit. */
    public sealed interface DrawOrder permits Line, Rect, SetLineWidth, SetColor {}

    public record Line(int x1, int y1, int x2, int y2) implements DrawOrder {}
    public record Rect(int x, int y, int width, int height) implements DrawOrder {}
    public record SetLineWidth(double widthDesignUnits) implements DrawOrder {}
    public record SetColor(String hexRgb) implements DrawOrder {}

    /**
     * Standard GOCA colour table (8-colour subset of {@code PCP} Basic Colour
     * Processing). Index 0xFF = "device default" which we interpret as black.
     */
    private static final String[] STANDARD_COLORS = new String[]{
            "#000000", // 0 black
            "#0000FF", // 1 blue
            "#FF0000", // 2 red
            "#FF00FF", // 3 magenta
            "#00FF00", // 4 green
            "#00FFFF", // 5 cyan
            "#FFFF00", // 6 yellow
            "#FFFFFF"  // 7 white
    };

    public static List<DrawOrder> decode(AfpGraphicObject graphic) {
        if (graphic == null || graphic.byteCount() == 0) return List.of();
        return decodeOrders(graphic.rawBytes());
    }

    static List<DrawOrder> decodeOrders(byte[] data) {
        List<DrawOrder> out = new ArrayList<>();
        int pos = 0;
        int currentX = 0;
        int currentY = 0;
        while (pos < data.length) {
            int op = data[pos] & 0xFF;
            // GCOMT (comment) and some vendor short orders use a 1-byte header
            // with no body — skip them quickly.
            if (op == 0x00) { pos++; continue; }
            // Short-form orders are 0x00..0x7F and carry a fixed number of
            // operand bytes baked into the opcode. Long-form orders are
            // 0x80..0xFF and carry a 1-byte length field after the opcode.
            int length;
            int bodyStart;
            if (op >= 0x80) {
                if (pos + 1 >= data.length) break;
                length = data[pos + 1] & 0xFF;
                bodyStart = pos + 2;
            } else {
                length = shortFormLength(op);
                bodyStart = pos + 1;
            }
            int bodyEnd = bodyStart + length;
            if (length < 0 || bodyEnd > data.length) break;

            switch (op) {
                case 0x21 -> {
                    // GSPS — set current position (2 bytes X, 2 bytes Y)
                    if (length >= 4) {
                        currentX = readI16(data, bodyStart);
                        currentY = readI16(data, bodyStart + 2);
                    }
                }
                case 0x81, 0xC1 -> {
                    // GLINE — line to point(s). Each point is 4 bytes.
                    int p = bodyStart;
                    while (p + 4 <= bodyEnd) {
                        int x2 = readI16(data, p);
                        int y2 = readI16(data, p + 2);
                        out.add(new Line(currentX, currentY, x2, y2));
                        currentX = x2;
                        currentY = y2;
                        p += 4;
                    }
                }
                case 0x80, 0xC0 -> {
                    // GBOX — corner to corner.
                    if (length >= 4) {
                        int x2 = readI16(data, bodyStart);
                        int y2 = readI16(data, bodyStart + 2);
                        int x = Math.min(currentX, x2);
                        int y = Math.min(currentY, y2);
                        int w = Math.abs(x2 - currentX);
                        int h = Math.abs(y2 - currentY);
                        out.add(new Rect(x, y, w, h));
                        currentX = x2;
                        currentY = y2;
                    }
                }
                case 0x19 -> {
                    // GSLW — set line width (1 byte unsigned, in design units).
                    if (length >= 1) {
                        out.add(new SetLineWidth(data[bodyStart] & 0xFF));
                    }
                }
                case 0x0A -> {
                    // GSCOL — set current colour by index.
                    if (length >= 1) {
                        int idx = data[bodyStart] & 0xFF;
                        String rgb = (idx < STANDARD_COLORS.length)
                                ? STANDARD_COLORS[idx]
                                : "#000000";
                        out.add(new SetColor(rgb));
                    }
                }
                default -> { /* tolerant skip */ }
            }
            pos = bodyEnd;
        }
        return out;
    }

    private static int shortFormLength(int op) {
        // The short-form GOCA orders we need: most carry no operand (segment
        // boundaries, chain markers). Treat unknown short-form as zero-length
        // and rely on subsequent bytes to be the next opcode.
        return switch (op) {
            case 0x21 -> 4;   // GSPS
            case 0x19 -> 1;   // GSLW
            case 0x0A -> 1;   // GSCOL
            default -> 0;
        };
    }

    private static int readI16(byte[] d, int off) {
        int v = ((d[off] & 0xFF) << 8) | (d[off + 1] & 0xFF);
        return (short) v; // sign-extend
    }
}
