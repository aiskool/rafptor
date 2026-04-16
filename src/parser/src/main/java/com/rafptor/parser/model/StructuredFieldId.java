package com.rafptor.parser.model;

import java.util.HexFormat;

/**
 * Three-byte Structured Field identifier: class / type / category.
 */
public record StructuredFieldId(int classByte, int typeByte, int categoryByte) {

    public StructuredFieldId {
        if ((classByte & ~0xFF) != 0 || (typeByte & ~0xFF) != 0 || (categoryByte & ~0xFF) != 0) {
            throw new IllegalArgumentException("StructuredFieldId bytes must be in [0,255]");
        }
    }

    public static StructuredFieldId of(int classByte, int typeByte, int categoryByte) {
        return new StructuredFieldId(classByte, typeByte, categoryByte);
    }

    public static StructuredFieldId fromBytes(byte[] bytes, int offset) {
        return new StructuredFieldId(bytes[offset] & 0xFF, bytes[offset + 1] & 0xFF, bytes[offset + 2] & 0xFF);
    }

    public int toInt() {
        return (classByte << 16) | (typeByte << 8) | categoryByte;
    }

    public String toHex() {
        return String.format("%02X %02X %02X", classByte, typeByte, categoryByte);
    }

    @Override
    public String toString() {
        return "StructuredFieldId[" + toHex() + "]";
    }

    public static StructuredFieldId parseHex(String hex) {
        byte[] b = HexFormat.of().parseHex(hex.replaceAll("\\s", ""));
        if (b.length != 3) {
            throw new IllegalArgumentException("Expected 3 hex bytes, got " + b.length);
        }
        return new StructuredFieldId(b[0] & 0xFF, b[1] & 0xFF, b[2] & 0xFF);
    }
}
