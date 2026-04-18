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
        this.defaultMapping = new FontMapping(
                "", "", "Default fallback", "IBM500",
                "Liberation Mono", "Helvetica",
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
        // Exact match first.
        for (FontMapping m : mappings) {
            if (upper.equalsIgnoreCase(m.afpCharsetPrefix())) {
                return m;
            }
        }
        // Prefix match either way — "C0N2" matches the "C0N200" mapping entry,
        // and "C0N20080" (a full charset name) also matches it.
        for (FontMapping m : mappings) {
            String candidate = m.afpCharsetPrefix();
            if (candidate == null || candidate.isEmpty()) continue;
            String candUpper = candidate.toUpperCase();
            if (candUpper.startsWith(upper) || upper.startsWith(candUpper)) {
                return m;
            }
        }
        return null;
    }

    public List<FontMapping> mappings() {
        return mappings;
    }
}
