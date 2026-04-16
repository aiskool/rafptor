package com.rafptor.parser.model;

/**
 * Raw (unparsed) Structured Field as produced by the RecordReader.
 *
 * <p>The {@code data} byte array is a defensive copy at construction time and is not
 * exposed by reference (accessors return a copy).
 */
public final class RawStructuredField {

    private final StructuredFieldId id;
    private final int flags;
    private final byte[] data;

    public RawStructuredField(StructuredFieldId id, int flags, byte[] data) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if ((flags & ~0xFF) != 0) {
            throw new IllegalArgumentException("flags must be in [0,255]");
        }
        this.id = id;
        this.flags = flags;
        this.data = data.clone();
    }

    public StructuredFieldId id() {
        return id;
    }

    public int flags() {
        return flags;
    }

    public byte[] data() {
        return data.clone();
    }

    public int dataLength() {
        return data.length;
    }
}
