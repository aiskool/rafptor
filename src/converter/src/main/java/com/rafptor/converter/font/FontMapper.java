package com.rafptor.converter.font;

public interface FontMapper {

    /**
     * Returns the mapping matching the given AFP code page and charset prefix.
     * Implementations must fall back to a default when no entry matches.
     */
    FontMapping map(String codePage, String charsetPrefix);

    /** Returns the default mapping used when no match is found. */
    FontMapping defaultMapping();

    /**
     * Look up a mapping by charset prefix alone (first 4 characters of the
     * coded-font resource name, e.g. {@code "C0H2"} for Courier).
     * Returns {@code null} when no entry matches.
     */
    default FontMapping findByCharsetPrefix(String prefix) {
        return null;
    }
}
