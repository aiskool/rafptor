package com.rafptor.parser.ptoca;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Map;

/**
 * Maps AFP code-page resource names (as carried by the MCF triplet X'85' /
 * FQN sub-type X'84') to JDK charset names that the JVM's built-in EBCDIC
 * decoders understand.
 *
 * <p>The naming convention in banking-grade AFP is
 * <code>T1V1NNNNN</code> → <code>IBMNNNNN</code> where <code>NNNNN</code> is
 * the numeric code-page identifier. We also handle a handful of producer-
 * specific synonyms (Doc1, Exstream, Xerox) and the generic
 * <code>T1D0BASE</code> used by IBM's sample fonts.
 *
 * <p>Any unknown name falls back to <code>IBM500</code> (International No. 5,
 * the most common default on AFP streams originating in Europe).
 */
public final class AfpCodePageMapper {

    private static final String FALLBACK_CHARSET = "IBM500";

    private static final Map<String, String> KNOWN = Map.ofEntries(
            Map.entry("T1V10037", "IBM037"),
            Map.entry("T1V10273", "IBM273"),
            Map.entry("T1V10277", "IBM277"),
            Map.entry("T1V10278", "IBM278"),
            Map.entry("T1V10280", "IBM280"),
            Map.entry("T1V10284", "IBM284"),
            Map.entry("T1V10285", "IBM285"),
            Map.entry("T1V10297", "IBM297"),
            Map.entry("T1V10500", "IBM500"),
            Map.entry("T1V10871", "IBM871"),
            Map.entry("T1V01140", "IBM1140"),
            Map.entry("T1V01141", "IBM1141"),
            Map.entry("T1V01142", "IBM1142"),
            Map.entry("T1V01143", "IBM1143"),
            Map.entry("T1V01144", "IBM1144"),
            Map.entry("T1V01145", "IBM1145"),
            Map.entry("T1V01146", "IBM1146"),
            Map.entry("T1V01147", "IBM1147"),
            Map.entry("T1V01148", "IBM1148"),
            Map.entry("T1V01149", "IBM1149"),
            Map.entry("T1000395", "IBM1047"),
            Map.entry("T1D0BASE", "IBM500"));

    private AfpCodePageMapper() { }

    /**
     * Resolve an AFP code-page resource name to a JVM charset name.
     * <p>Resolution order:
     * <ol>
     *   <li>Explicit mapping table.</li>
     *   <li>Extract a numeric suffix (e.g. <code>T1V10037</code> → 37) and check
     *       whether <code>IBMNNN</code> is supported by the running JVM.</li>
     *   <li>Fallback <code>IBM500</code>.</li>
     * </ol>
     * Returns a non-null, non-empty JVM charset name.
     */
    public static String resolve(String afpCodePageName) {
        if (afpCodePageName == null) {
            return FALLBACK_CHARSET;
        }
        String trimmed = afpCodePageName.trim().toUpperCase();
        if (trimmed.isEmpty()) {
            return FALLBACK_CHARSET;
        }
        String explicit = KNOWN.get(trimmed);
        if (explicit != null) {
            return explicit;
        }
        // Try to extract a numeric suffix and probe IBMxxx.
        int suffix = extractNumericSuffix(trimmed);
        if (suffix > 0) {
            String candidate = "IBM" + suffix;
            if (isCharsetSupported(candidate)) {
                return candidate;
            }
            String candidate4 = String.format("IBM%04d", suffix);
            if (!candidate4.equals(candidate) && isCharsetSupported(candidate4)) {
                return candidate4;
            }
        }
        return FALLBACK_CHARSET;
    }

    /** Resolve then return the {@link Charset}. Never throws. */
    public static Charset resolveCharset(String afpCodePageName) {
        String name = resolve(afpCodePageName);
        try {
            return Charset.forName(name);
        } catch (RuntimeException e) {
            return Charset.forName(FALLBACK_CHARSET);
        }
    }

    private static int extractNumericSuffix(String s) {
        int end = s.length();
        int start = end;
        while (start > 0 && Character.isDigit(s.charAt(start - 1))) {
            start--;
        }
        if (start == end) {
            return -1;
        }
        try {
            return Integer.parseInt(s.substring(start));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static boolean isCharsetSupported(String name) {
        try {
            return Charset.isSupported(name);
        } catch (IllegalCharsetNameException e) {
            return false;
        }
    }
}
