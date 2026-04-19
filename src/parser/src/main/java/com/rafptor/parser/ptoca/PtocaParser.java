package com.rafptor.parser.ptoca;

import com.rafptor.parser.modca.PresentationTextData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a PTOCA control-sequence stream.
 *
 * <p>PTOCA control sequences use two length encodings:
 * <ul>
 *   <li><b>Chained-introducer form</b>: {@code 2B D3 LL FN …} — the MO:DCA
 *       standard. {@code 2B D3} is the class+type introducer, {@code LL} is
 *       the length byte covering {@code LL+FN+payload} (so payload length is
 *       {@code LL-2}), and {@code FN} is the function class byte whose
 *       least-significant bit is the chaining flag (LSB=1 → another chained
 *       sequence follows immediately). The actual opcode is
 *       {@code FN & 0xFE}.</li>
 *   <li><b>Unchained form</b>: {@code LL FN …} — a length-prefixed CS with no
 *       introducer. Used by the Rafptor simulator for simplicity and by some
 *       legacy producers.</li>
 * </ul>
 *
 * <p>Every unrecognised sequence is skipped using its declared length — we never
 * guess content boundaries.
 *
 * <p><b>Security:</b> the parser never logs decoded text (potential client PII).
 * Only structural counters are emitted at DEBUG level.
 */
public final class PtocaParser {

    private static final Logger LOG = LoggerFactory.getLogger(PtocaParser.class);

    private static final int INTRODUCER_1 = 0x2B;
    private static final int INTRODUCER_2 = 0xD3;

    private final EbcdicDecoder decoder;

    public PtocaParser() {
        this(EbcdicDecoder.ibm500());
    }

    public PtocaParser(EbcdicDecoder decoder) {
        if (decoder == null) {
            throw new IllegalArgumentException("decoder must not be null");
        }
        this.decoder = decoder;
    }

    /**
     * Grouping record returned by {@link #parseWithRules} — text runs and
     * drawn rules share the same PTOCA stream but consumer code typically
     * handles them through different IR elements.
     */
    public record Result(List<PtocaTextRun> runs, List<PtocaRule> rules) {
        public Result {
            runs = List.copyOf(runs);
            rules = List.copyOf(rules);
        }
    }

    public List<PtocaTextRun> parse(PresentationTextData ptx) {
        if (ptx == null) {
            throw new IllegalArgumentException("ptx must not be null");
        }
        return parseBytes(ptx.payload(), Map.of());
    }

    /**
     * Parse a PTOCA payload with per-local-id code-page hints from the MCF.
     * When a SCFL sequence selects a font id that has an associated AFP
     * code-page name in {@code codePageByLocalId}, subsequent TRN sequences
     * are decoded with the matching JVM EBCDIC charset; otherwise the parser
     * falls back to its default decoder (IBM500).
     */
    public List<PtocaTextRun> parse(PresentationTextData ptx, Map<Integer, String> codePageByLocalId) {
        if (ptx == null) {
            throw new IllegalArgumentException("ptx must not be null");
        }
        return parseBytes(ptx.payload(), codePageByLocalId);
    }

    /** Full parse returning both text runs and rules drawn via DIR/DBR. */
    public Result parseWithRules(PresentationTextData ptx, Map<Integer, String> codePageByLocalId) {
        if (ptx == null) {
            throw new IllegalArgumentException("ptx must not be null");
        }
        return parseBytesWithRules(ptx.payload(), codePageByLocalId);
    }

    public List<PtocaTextRun> parseBytes(byte[] data) {
        return parseBytes(data, Map.of());
    }

    public List<PtocaTextRun> parseBytes(byte[] data, Map<Integer, String> codePageByLocalId) {
        return parseBytesWithRules(data, codePageByLocalId).runs();
    }

    public Result parseBytesWithRules(byte[] data, Map<Integer, String> codePageByLocalId) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        Map<Integer, String> codePages = codePageByLocalId == null ? Map.of() : codePageByLocalId;
        Map<Integer, EbcdicDecoder> decoderCache = new HashMap<>();
        List<PtocaTextRun> runs = new ArrayList<>();
        List<PtocaRule> rules = new ArrayList<>();
        int localFontId = 0;
        int baseline = 0;
        int inline = 0;
        String currentColor = "#000000";
        EbcdicDecoder currentDecoder = resolveDecoder(localFontId, codePages, decoderCache);

        int pos = 0;
        int sequenceCount = 0;
        while (pos < data.length) {
            int header = data[pos] & 0xFF;
            int opcode;
            int length;
            int payloadOffset;
            int sequenceStart = pos;

            if (header == INTRODUCER_1
                    && pos + 3 < data.length
                    && (data[pos + 1] & 0xFF) == INTRODUCER_2) {
                // Chained-introducer form: 2B D3 LL FN …
                length = data[pos + 2] & 0xFF;
                int fn = data[pos + 3] & 0xFF;
                opcode = fn & 0xFE;  // low bit is the chaining flag
                payloadOffset = pos + 4;
                if (length < 2) {
                    break; // padding / truncation
                }
            } else {
                // Unchained form: LL FN …
                length = header;
                if (length < 2 || pos + 1 >= data.length) {
                    break;
                }
                int fn = data[pos + 1] & 0xFF;
                // The function-class byte's LSB is the "chained" flag: when a
                // PTOCA stream is opened with 2B D3 every subsequent CS may
                // still carry the flag (FN with LSB=1). Mask it away so the
                // real opcode (e.g. TRN 0xDA seen as 0xDB, AMI 0xC6 as 0xC7)
                // lands on the switch below.
                opcode = fn & 0xFE;
                payloadOffset = pos + 2;
            }

            int sequenceEnd = payloadOffset + (length - 2);
            if (sequenceEnd > data.length) {
                break;
            }

            switch (opcode) {
                case PtocaControlCode.SET_CODED_FONT_LOCAL -> {
                    if (payloadOffset < sequenceEnd) {
                        localFontId = data[payloadOffset] & 0xFF;
                        currentDecoder = resolveDecoder(localFontId, codePages, decoderCache);
                    }
                }
                case PtocaControlCode.ABSOLUTE_MOVE_BASELINE -> baseline = readUnsignedShort(data, payloadOffset, sequenceEnd);
                case PtocaControlCode.ABSOLUTE_MOVE_INLINE -> inline = readUnsignedShort(data, payloadOffset, sequenceEnd);
                case PtocaControlCode.RELATIVE_MOVE_INLINE -> inline += readUnsignedShort(data, payloadOffset, sequenceEnd);
                case PtocaControlCode.TRANSPARENT_DATA -> {
                    int textLen = sequenceEnd - payloadOffset;
                    if (textLen > 0) {
                        String text = decodeTrn(data, payloadOffset, textLen, currentDecoder);
                        runs.add(new PtocaTextRun(localFontId, baseline, inline, text, currentColor));
                    }
                }
                case PtocaControlCode.SET_EXTENDED_COLOR -> {
                    String parsed = parseExtendedColor(data, payloadOffset, sequenceEnd);
                    if (parsed != null) {
                        currentColor = parsed;
                    }
                }
                case PtocaControlCode.DRAW_I_AXIS_RULE -> {
                    PtocaRule rule = parseRule(data, payloadOffset, sequenceEnd,
                            baseline, inline, PtocaRule.Direction.I_AXIS, currentColor);
                    if (rule != null) rules.add(rule);
                }
                case PtocaControlCode.DRAW_B_AXIS_RULE -> {
                    PtocaRule rule = parseRule(data, payloadOffset, sequenceEnd,
                            baseline, inline, PtocaRule.Direction.B_AXIS, currentColor);
                    if (rule != null) rules.add(rule);
                }
                default -> {
                    // tolerant skip
                }
            }
            sequenceCount++;
            if (sequenceEnd <= sequenceStart) {
                // Defensive against zero-advance loops on malformed input.
                break;
            }
            pos = sequenceEnd;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("ptoca sequences={} runs={} rules={}", sequenceCount, runs.size(), rules.size());
        }
        return new Result(runs, rules);
    }

    /**
     * Parse a DIR / DBR rule payload. MO:DCA/P5 producers emit a 5-byte
     * payload where the rule width (thickness) is a <b>2-byte</b> big-endian
     * value at offset 2..3 — not a 1-byte field at offset 3. Wire layout:
     * <pre>
     *   [0..1]  rule length in L-units   (big-endian, max 65535)
     *   [2..3]  rule width in L-units    (big-endian)
     *   [4]     flags / pad (observed 0x00)
     * </pre>
     * Legacy 4-byte payloads use a single byte thickness at offset 3; when
     * the payload is shorter, thickness defaults to 15 L-units (≈ 0.75 pt).
     */
    private static PtocaRule parseRule(byte[] data, int offset, int end,
                                       int baseline, int inline,
                                       PtocaRule.Direction direction,
                                       String color) {
        int len = end - offset;
        if (len < 2) return null;
        int length = readUnsignedShort(data, offset, end);
        int thickness;
        if (len >= 5) {
            thickness = readUnsignedShort(data, offset + 2, end);
        } else if (len >= 4) {
            thickness = data[offset + 3] & 0xFF;
        } else {
            thickness = 15;
        }
        if (length <= 0) return null;
        if (thickness <= 0) thickness = 15;
        return new PtocaRule(baseline, inline, length, thickness, direction, color);
    }

    private static int readUnsignedShort(byte[] data, int offset, int end) {
        if (offset + 1 >= end) {
            return 0;
        }
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    /**
     * Decode a TRN payload. Recent MO:DCA/P5 producers emit Unicode code points
     * (UTF-16BE) inside the TRN bytes when the coded font is a TrueType/OpenType
     * resource. We auto-detect that case by looking at the payload shape — an
     * even length with a strong zero-high-byte signal is a reliable tell.
     * Fallback: the EBCDIC decoder configured from the MCF code page.
     */
    private static String decodeTrn(byte[] data, int offset, int length, EbcdicDecoder ebcdic) {
        if (length >= 2 && (length % 2) == 0 && looksLikeUtf16BE(data, offset, length)) {
            return new String(data, offset, length, java.nio.charset.StandardCharsets.UTF_16BE);
        }
        return ebcdic.decode(data, offset, length);
    }

    /**
     * Heuristic: Latin-script UTF-16BE text has a high byte of 0x00 for every
     * code point in the ASCII/Latin-1 range. We sample code units and require
     * {@code zeroHigh * 4 >= codeUnits * 3} to commit to UTF-16BE.
     *
     * <p>For 2-byte TRNs (single UTF-16 code unit — very common when composers
     * emit one-character words like "a" or "I"), we apply a stricter rule:
     * the high byte must be 0x00 and the low byte must be a printable ASCII
     * character. That rule prevents EBCDIC single-char TRNs from being
     * mis-detected as Unicode — and, more importantly, prevents the reverse
     * mistake where a Unicode single-char "a" (0x00 0x61) falls through to
     * EBCDIC and renders as "/" (IBM500 code point 0x61 = '/').
     */
    private static boolean looksLikeUtf16BE(byte[] data, int offset, int length) {
        if (length == 2) {
            int high = data[offset] & 0xFF;
            int low = data[offset + 1] & 0xFF;
            return high == 0x00 && low >= 0x20 && low < 0x7F;
        }
        if (length < 4) {
            return false;
        }
        int codeUnits = length / 2;
        int zeroHigh = 0;
        for (int i = 0; i < codeUnits; i++) {
            if ((data[offset + 2 * i] & 0xFF) == 0x00) {
                zeroHigh++;
            }
        }
        return zeroHigh * 4 >= codeUnits * 3;
    }

    /**
     * Parse a PTOCA Set Extended Color (SEC) payload. We support the
     * 13-byte RGB variant (color space 0x01) that IBM, Doc1, Adobe Output
     * and Compart all emit for foreground color changes. Wire layout
     * (after the 2-byte CS header):
     * <pre>
     *   [0]     reserved (0x00)
     *   [1]     color space (0x01 = RGB)
     *   [2..5]  reserved
     *   [6]     bits per R  (typically 0x08)
     *   [7]     bits per G  (typically 0x08)
     *   [8]     bits per B  (typically 0x08)
     *   [9]     bits per reserved component
     *   [10]    R (0..255)
     *   [11]    G (0..255)
     *   [12]    B (0..255)
     * </pre>
     * Returns {@code null} (leaves caller's color untouched) if the payload
     * is not the 13-byte RGB variant — named-color tables and CMYK are not
     * yet modelled.
     */
    private static String parseExtendedColor(byte[] data, int offset, int end) {
        int len = end - offset;
        if (len < 13) {
            return null;
        }
        int colorSpace = data[offset + 1] & 0xFF;
        if (colorSpace != 0x01) {
            return null;
        }
        int r = data[offset + 10] & 0xFF;
        int g = data[offset + 11] & 0xFF;
        int b = data[offset + 12] & 0xFF;
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private EbcdicDecoder resolveDecoder(int localFontId,
                                         Map<Integer, String> codePages,
                                         Map<Integer, EbcdicDecoder> cache) {
        EbcdicDecoder cached = cache.get(localFontId);
        if (cached != null) {
            return cached;
        }
        String afpName = codePages.get(localFontId);
        if (afpName == null || afpName.isBlank()) {
            cache.put(localFontId, decoder);
            return decoder;
        }
        String jdkName = AfpCodePageMapper.resolve(afpName);
        EbcdicDecoder resolved;
        try {
            resolved = new EbcdicDecoder(jdkName);
        } catch (RuntimeException e) {
            resolved = decoder;
        }
        cache.put(localFontId, resolved);
        return resolved;
    }
}
