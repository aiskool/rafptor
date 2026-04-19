package com.rafptor.converter.font;

import java.util.List;
import java.util.Objects;

public final class StandardFontMapper implements FontMapper {

    private final List<FontMapping> mappings;
    private final FontMapping defaultMapping;

    public StandardFontMapper() {
        this(FontMappingRegistry.loadDefault());
    }

    public StandardFontMapper(List<FontMapping> mappings) {
        this.mappings = mappings == null ? List.of() : List.copyOf(mappings);
        // Default fallback: Liberation Sans has Arial-equivalent metrics so
        // streams that declare a TrueType font via MDR (without a Coded Font
        // resource we can map) land on an Arial-like face by default instead
        // of a monospace that forces every glyph into a fixed-pitch cell.
        this.defaultMapping = new FontMapping(
                "", "", "Default fallback", "IBM500",
                "Liberation Sans", "Helvetica",
                1.0, 0.0, 10.0, FontMetrics.defaults());
    }

    @Override
    public FontMapping map(String codePage, String charsetPrefix) {
        if (codePage != null && charsetPrefix != null) {
            for (FontMapping m : mappings) {
                if (Objects.equals(codePage, m.afpCodePage())
                        && Objects.equals(charsetPrefix, m.afpCharsetPrefix())) {
                    return m;
                }
            }
        }
        if (codePage != null) {
            for (FontMapping m : mappings) {
                if (Objects.equals(codePage, m.afpCodePage())) {
                    return m;
                }
            }
        }
        return defaultMapping;
    }

    @Override
    public FontMapping defaultMapping() {
        return defaultMapping;
    }

    @Override
    public FontMapping findByCharsetPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return null;
        String upper = prefix.toUpperCase();
        // Exact match wins.
        for (FontMapping m : mappings) {
            if (upper.equalsIgnoreCase(m.afpCharsetPrefix())) {
                return m;
            }
        }
        // Otherwise keep the longest mapping-prefix that is a prefix of the
        // lookup key. A full charset name "C0N20080" must select "C0N200"
        // rather than "C0N" so that producers that use the official 8-byte
        // IBM coded-font names land on the right family without tripping on
        // shorter generic families.
        FontMapping best = null;
        int bestLen = -1;
        for (FontMapping m : mappings) {
            String candidate = m.afpCharsetPrefix();
            if (candidate == null || candidate.isEmpty()) continue;
            String candUpper = candidate.toUpperCase();
            if (upper.startsWith(candUpper) && candUpper.length() > bestLen) {
                best = m;
                bestLen = candUpper.length();
            }
        }
        if (best != null) return best;
        // Reverse direction — a short lookup ("C0N2") should still reach the
        // longer mapping entry ("C0N200") when the caller has not decoded the
        // full 8-byte name.
        for (FontMapping m : mappings) {
            String candidate = m.afpCharsetPrefix();
            if (candidate == null || candidate.isEmpty()) continue;
            String candUpper = candidate.toUpperCase();
            if (candUpper.startsWith(upper)) {
                return m;
            }
        }
        return null;
    }

    public List<FontMapping> mappings() {
        return mappings;
    }
}
