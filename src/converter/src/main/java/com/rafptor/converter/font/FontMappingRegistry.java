package com.rafptor.converter.font;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minimal, dependency-free JSON loader for the font-mapping registry.
 *
 * <p>The file format is a fixed subset used only here. We deliberately avoid
 * pulling in Jackson/Gson: one dep fewer for a piece of data that changes once
 * a year.
 */
public final class FontMappingRegistry {

    public static final String DEFAULT_RESOURCE = "/font-mappings/standard-mappings.json";

    private FontMappingRegistry() {
    }

    /** Loads the default mappings embedded in the jar. */
    public static List<FontMapping> loadDefault() {
        try (InputStream in = FontMappingRegistry.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                return List.of();
            }
            return parse(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + DEFAULT_RESOURCE, e);
        }
    }

    /** Parses a mapping document. Exposed for tests and callers with custom content. */
    public static List<FontMapping> parse(String json) {
        List<FontMapping> result = new ArrayList<>();
        int mappingsIdx = json.indexOf("\"mappings\"");
        if (mappingsIdx < 0) {
            return result;
        }
        int arrayStart = json.indexOf('[', mappingsIdx);
        int arrayEnd = findMatching(json, arrayStart, '[', ']');
        if (arrayStart < 0 || arrayEnd < 0) {
            return result;
        }
        int cursor = arrayStart + 1;
        while (cursor < arrayEnd) {
            int objStart = json.indexOf('{', cursor);
            if (objStart < 0 || objStart > arrayEnd) {
                break;
            }
            int objEnd = findMatching(json, objStart, '{', '}');
            if (objEnd < 0) {
                break;
            }
            String obj = json.substring(objStart, objEnd + 1);
            result.add(parseMapping(obj));
            cursor = objEnd + 1;
        }
        return Collections.unmodifiableList(result);
    }

    private static FontMapping parseMapping(String obj) {
        String afpCodePage = str(obj, "afp_codepage");
        String afpCharsetPrefix = str(obj, "afp_charset_prefix");
        String description = str(obj, "description");
        String ebcdicEncoding = str(obj, "ebcdic_encoding");
        String truetypeFont = str(obj, "truetype_font");
        String fallbackFont = str(obj, "fallback_font");
        double scale = num(obj, "scale_factor", 1.0);
        double baseline = num(obj, "baseline_offset", 0.0);
        double size = num(obj, "default_point_size", 10.0);
        return new FontMapping(
                nz(afpCodePage),
                nz(afpCharsetPrefix),
                nz(description),
                nz(ebcdicEncoding, "IBM500"),
                nz(truetypeFont, "Liberation Mono"),
                nz(fallbackFont, "Helvetica"),
                scale,
                baseline,
                size,
                FontMetrics.defaults());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String nz(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    private static String str(String obj, String key) {
        String needle = "\"" + key + "\"";
        int k = obj.indexOf(needle);
        if (k < 0) {
            return null;
        }
        int colon = obj.indexOf(':', k + needle.length());
        if (colon < 0) {
            return null;
        }
        int qStart = obj.indexOf('"', colon + 1);
        if (qStart < 0) {
            return null;
        }
        int qEnd = qStart + 1;
        StringBuilder out = new StringBuilder();
        while (qEnd < obj.length()) {
            char c = obj.charAt(qEnd);
            if (c == '\\' && qEnd + 1 < obj.length()) {
                char next = obj.charAt(qEnd + 1);
                switch (next) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    default -> out.append(next);
                }
                qEnd += 2;
                continue;
            }
            if (c == '"') {
                return out.toString();
            }
            out.append(c);
            qEnd++;
        }
        return out.toString();
    }

    private static double num(String obj, String key, double fallback) {
        String needle = "\"" + key + "\"";
        int k = obj.indexOf(needle);
        if (k < 0) {
            return fallback;
        }
        int colon = obj.indexOf(':', k + needle.length());
        if (colon < 0) {
            return fallback;
        }
        int i = colon + 1;
        while (i < obj.length() && Character.isWhitespace(obj.charAt(i))) {
            i++;
        }
        int start = i;
        while (i < obj.length()) {
            char c = obj.charAt(i);
            if ((c >= '0' && c <= '9') || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E') {
                i++;
            } else {
                break;
            }
        }
        if (start == i) {
            return fallback;
        }
        try {
            return Double.parseDouble(obj.substring(start, i));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int findMatching(String s, int openIdx, char open, char close) {
        if (openIdx < 0 || openIdx >= s.length() || s.charAt(openIdx) != open) {
            return -1;
        }
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = openIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
