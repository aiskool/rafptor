package com.rafptor.parser;

/**
 * Hard limits applied during AFP parsing.
 *
 * <p>AFP streams are provided by clients and MUST be treated as potentially hostile.
 * Every length-prefixed allocation is bounded by one of these limits.
 */
public final class ParserLimits {

    public static final int DEFAULT_MAX_FIELD_SIZE = 1_048_576;
    public static final long DEFAULT_MAX_DOCUMENT_SIZE = 10L * 1024L * 1024L * 1024L;
    public static final int DEFAULT_MAX_NESTING_DEPTH = 20;
    public static final long DEFAULT_PARSE_TIMEOUT_MILLIS = 300_000L;

    private final int maxFieldSize;
    private final long maxDocumentSize;
    private final int maxNestingDepth;
    private final long parseTimeoutMillis;

    public ParserLimits(int maxFieldSize, long maxDocumentSize, int maxNestingDepth, long parseTimeoutMillis) {
        if (maxFieldSize <= 0) {
            throw new IllegalArgumentException("maxFieldSize must be > 0");
        }
        if (maxDocumentSize <= 0L) {
            throw new IllegalArgumentException("maxDocumentSize must be > 0");
        }
        if (maxNestingDepth <= 0) {
            throw new IllegalArgumentException("maxNestingDepth must be > 0");
        }
        if (parseTimeoutMillis <= 0L) {
            throw new IllegalArgumentException("parseTimeoutMillis must be > 0");
        }
        this.maxFieldSize = maxFieldSize;
        this.maxDocumentSize = maxDocumentSize;
        this.maxNestingDepth = maxNestingDepth;
        this.parseTimeoutMillis = parseTimeoutMillis;
    }

    public static ParserLimits defaults() {
        return new ParserLimits(
                DEFAULT_MAX_FIELD_SIZE,
                DEFAULT_MAX_DOCUMENT_SIZE,
                DEFAULT_MAX_NESTING_DEPTH,
                DEFAULT_PARSE_TIMEOUT_MILLIS);
    }

    public int maxFieldSize() {
        return maxFieldSize;
    }

    public long maxDocumentSize() {
        return maxDocumentSize;
    }

    public int maxNestingDepth() {
        return maxNestingDepth;
    }

    public long parseTimeoutMillis() {
        return parseTimeoutMillis;
    }
}
