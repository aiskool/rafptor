package com.rafptor.parser.model;

import com.rafptor.parser.audit.SfRegistry;

import java.util.Objects;

/**
 * A structured field whose identifier is not currently parsed by Rafptor.
 *
 * <p>Instead of being dropped, the raw body is preserved here so an operator
 * can inspect it after the parse. The body is truncated to {@code MAX_BODY_BYTES}
 * to avoid OOM on pathological inputs; {@link #truncated()} indicates whether
 * truncation happened.
 */
public final class OpaqueSf {

    public static final int MAX_BODY_BYTES = 64 * 1024;

    private final long offset;
    private final String idHex;
    private final int pageIndex;
    private final byte[] body;
    private final int originalLength;

    public OpaqueSf(long offset, String idHex, int pageIndex, byte[] body) {
        if (idHex == null) {
            throw new IllegalArgumentException("idHex must not be null");
        }
        if (body == null) {
            throw new IllegalArgumentException("body must not be null");
        }
        this.offset = offset;
        this.idHex = idHex.toUpperCase();
        this.pageIndex = pageIndex;
        this.originalLength = body.length;
        if (body.length > MAX_BODY_BYTES) {
            byte[] truncated = new byte[MAX_BODY_BYTES];
            System.arraycopy(body, 0, truncated, 0, MAX_BODY_BYTES);
            this.body = truncated;
        } else {
            this.body = body.clone();
        }
    }

    public long offset() { return offset; }
    public String idHex() { return idHex; }
    public int pageIndex() { return pageIndex; }
    public int originalLength() { return originalLength; }
    public boolean truncated() { return originalLength > MAX_BODY_BYTES; }
    public byte[] body() { return body.clone(); }

    /** Mnemonic from the SfRegistry (falls back to "UNKNOWN"). */
    public String humanReadableId() {
        return SfRegistry.lookup(idHex).map(i -> i.mnemonic()).orElse("UNKNOWN");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OpaqueSf other)) return false;
        return offset == other.offset && idHex.equals(other.idHex)
                && originalLength == other.originalLength && pageIndex == other.pageIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, idHex, originalLength, pageIndex);
    }
}
