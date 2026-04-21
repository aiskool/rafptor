package com.rafptor.parser.ptoca;

import com.rafptor.parser.audit.OpcodeInfo;
import com.rafptor.parser.audit.PtocaByteAccountant;
import com.rafptor.parser.audit.PtocaOpcodeRegistry;
import com.rafptor.parser.audit.PtocaOpcodeReport;
import com.rafptor.parser.modca.PresentationTextData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public record Result(List<PtocaTextRun> runs, List<PtocaRule> rules,
                         PtocaOpcodeReport opcodeReport) {
        public Result {
            runs = List.copyOf(runs);
            rules = List.copyOf(rules);
        }
        // Backward-compatible 2-arg constructor — report defaults to null.
        public Result(List<PtocaTextRun> runs, List<PtocaRule> rules) {
            this(runs, rules, null);
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
        PtocaByteAccountant opAcc = new PtocaByteAccountant();
        opAcc.setTotalPtxBytes(data.length);
        Set<Integer> warnedOpcodes = new HashSet<>();
        int orientationDegrees = 0;
        boolean underscored = false;
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
                        runs.add(new PtocaTextRun(localFontId, baseline, inline, text,
                                currentColor, orientationDegrees, underscored));
                        // PTOCA TRN spec: after a TRN, the inline cursor must
                        // advance by the sum of character increments so that
                        // subsequent RMI values operate from the correct
                        // post-text position. For raster fonts the exact
                        // increments live in the FNI but Rafptor substitutes
                        // Liberation — approximate with a conservative
                        // 12 L-units/char (≈ 10 cpi at 1440 L-units/inch).
                        // This matters for IBM TOC layouts that emit
                        // "TRN Title" → "RMI +N" → "TRN PageNumber" on the
                        // same baseline and expect the number to land AFTER
                        // the title, not inside it. 14 L-units = ~9 cpi at
                        // 1440 L-units/inch, close enough to Times/Helvetica
                        // proportional averages and to 10-cpi Courier alike.
                        inline += text.length() * 14;
                    }
                }
                case PtocaControlCode.SET_TEXT_ORIENTATION -> {
                    // STO payload: 4 bytes [I-rot 2B][B-rot 2B] where the
                    // high byte encodes 90° steps: 0x00=0°, 0x2D=90°, 0x5A=180°,
                    // 0x87=270°. We only track the I-axis (inline) angle.
                    int iAxisHi = (payloadOffset + 1 < sequenceEnd)
                            ? data[payloadOffset] & 0xFF
                            : 0;
                    orientationDegrees = switch (iAxisHi) {
                        case 0x2D -> 90;
                        case 0x5A -> 180;
                        case 0x87 -> 270;
                        default   -> 0;
                    };
                }
                case PtocaControlCode.UNDERSCORE -> {
                    int flag = (payloadOffset < sequenceEnd) ? data[payloadOffset] & 0xFF : 0;
                    underscored = (flag != 0);
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
            // Accounting: track every CS regardless of whether we acted on it.
            OpcodeInfo opInfo = PtocaOpcodeRegistry.lookup(opcode).orElse(OpcodeInfo.UNKNOWN);
            PtocaByteAccountant.OpStatus status = opInfo == OpcodeInfo.UNKNOWN
                    ? PtocaByteAccountant.OpStatus.UNKNOWN
                    : (opInfo.implemented()
                            ? PtocaByteAccountant.OpStatus.USED
                            : PtocaByteAccountant.OpStatus.IGNORED);
            opAcc.register(sequenceStart, sequenceEnd - sequenceStart,
                    opcode, opInfo.mnemonic(), status);
            if (status == PtocaByteAccountant.OpStatus.UNKNOWN
                    && warnedOpcodes.add(opcode)) {
                LOG.warn("unknown PTOCA opcode 0x{} at offset {} — skipped tolerantly",
                        String.format("%02X", opcode), sequenceStart);
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
        return new Result(runs, rules, opAcc.generateReport());
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
     * Decode a TRN payload. We run three heuristics in order:
     *
     * <ol>
     *   <li><b>UTF-16BE</b> — MO:DCA-P5 producers that embed TrueType fonts
     *       via MDR emit Unicode code points directly. Even length plus a
     *       strong zero-high-byte signal commits.</li>
     *   <li><b>ASCII</b> — many modern producers (xafp, Infoprint Designer,
     *       some Ricoh output) route plain ASCII text through PTOCA even
     *       when the MCF declares a raster font. If &gt;= 80% of the bytes
     *       fall in the printable-ASCII range {@code 0x20..0x7E} we decode
     *       as ISO-8859-1 (US-ASCII superset) — that yields correct Latin-1
     *       text for accented glyphs too.</li>
     *   <li><b>EBCDIC</b> — the traditional mainframe path, driven by the
     *       MCF code-page triplet and the {@link AfpCodePageMapper}.</li>
     * </ol>
     *
     * This ordering makes the "fallback CP500" path only apply to documents
     * that actually speak EBCDIC, so a 99%-ASCII producer no longer renders
     * every glyph as "?".
     */
    private static String decodeTrn(byte[] data, int offset, int length, EbcdicDecoder ebcdic) {
        if (length >= 2 && (length % 2) == 0 && looksLikeUtf16BE(data, offset, length)) {
            return new String(data, offset, length, java.nio.charset.StandardCharsets.UTF_16BE);
        }
        if (looksLikeAscii(data, offset, length)) {
            return new String(data, offset, length, java.nio.charset.StandardCharsets.ISO_8859_1);
        }
        return ebcdic.decode(data, offset, length);
    }

    /**
     * Commit to ASCII / ISO-8859-1 when at least 80% of the bytes fall in the
     * printable-ASCII band [0x20..0x7E] with the usual whitespace escapes.
     * A single-byte TRN of 'A'..'z' also qualifies. Intentionally stricter
     * than a raw "all bytes &lt; 0x80" test — EBCDIC punctuation bytes like
     * 0x40 (space) must still prefer the EBCDIC path when the surrounding
     * content is EBCDIC-shaped.
     */
    private static boolean looksLikeAscii(byte[] data, int offset, int length) {
        if (length <= 0) return false;
        int printable = 0;
        int high = 0;
        for (int i = 0; i < length; i++) {
            int b = data[offset + i] & 0xFF;
            if (b >= 0x20 && b < 0x7F) {
                printable++;
            } else if (b == 0x09 || b == 0x0A || b == 0x0D) {
                printable++;
            } else if (b >= 0x80) {
                high++;
            }
        }
        // Hard guard: no bytes above 0x7F + at least 80% printable ASCII.
        if (high > 0) return false;
        return printable * 5 >= length * 4;
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
        if (len < 2) {
            return null;
        }
        int colorSpace = data[offset + 1] & 0xFF;
        return switch (colorSpace) {
            // RGB: 13-byte payload, components at offsets 10..12.
            case 0x01 -> len >= 13
                    ? String.format("#%02X%02X%02X",
                            data[offset + 10] & 0xFF,
                            data[offset + 11] & 0xFF,
                            data[offset + 12] & 0xFF)
                    : null;
            // CMYK: 14-byte payload, components at offsets 10..13 (% 0..255).
            case 0x04 -> len >= 14
                    ? cmykToRgb(data[offset + 10] & 0xFF,
                                data[offset + 11] & 0xFF,
                                data[offset + 12] & 0xFF,
                                data[offset + 13] & 0xFF)
                    : null;
            // Highlight color: 2-byte palette index at offset 10..11.
            case 0x06 -> len >= 12
                    ? highlightToRgb(((data[offset + 10] & 0xFF) << 8)
                                     | (data[offset + 11] & 0xFF))
                    : null;
            // CIELAB: 3 * 2 bytes big-endian at offsets 10..15 (L* a* b*).
            case 0x08 -> len >= 16
                    ? cielabToRgb(
                            readS16(data, offset + 10),
                            readS16(data, offset + 12),
                            readS16(data, offset + 14))
                    : null;
            default -> null;
        };
    }

    private static int readS16(byte[] data, int p) {
        int v = ((data[p] & 0xFF) << 8) | (data[p + 1] & 0xFF);
        return (short) v;
    }

    /** CMYK → RGB via the straight subtractive formula. */
    static String cmykToRgb(int c, int m, int y, int k) {
        double cc = c / 255.0, mm = m / 255.0, yy = y / 255.0, kk = k / 255.0;
        int r = (int) Math.round(255 * (1 - cc) * (1 - kk));
        int g = (int) Math.round(255 * (1 - mm) * (1 - kk));
        int b = (int) Math.round(255 * (1 - yy) * (1 - kk));
        return String.format("#%02X%02X%02X",
                Math.min(255, Math.max(0, r)),
                Math.min(255, Math.max(0, g)),
                Math.min(255, Math.max(0, b)));
    }

    /** AFP Highlight Color palette index → sRGB approximation. */
    static String highlightToRgb(int index) {
        String[] palette = {
                "#000000", // 0 default (black)
                "#2196F3", // 1 blue
                "#E53935", // 2 red
                "#E91E63", // 3 magenta / pink
                "#43A047", // 4 green
                "#00BCD4", // 5 cyan / turquoise
                "#FDD835", // 6 yellow
                "#FFFFFF"  // 7 white (non-print)
        };
        if (index >= 0 && index < palette.length) return palette[index];
        return "#000000";
    }

    /** CIELAB (L* a* b*) → sRGB via D65 reference white. */
    static String cielabToRgb(int L, int a, int b) {
        // Convert to CIE L*a*b* float (L: 0..100, a/b: ~-128..+127).
        double Lf = L / 100.0 * 100.0;
        double af = a;
        double bf = b;
        // L*a*b* → XYZ
        double y = (Lf + 16) / 116.0;
        double x = af / 500.0 + y;
        double z = y - bf / 200.0;
        x = refMul(x) * 95.047;
        y = refMul(y) * 100.000;
        z = refMul(z) * 108.883;
        // XYZ → sRGB (D65)
        double rr =  x *  3.2406 / 100 + y * -1.5372 / 100 + z * -0.4986 / 100;
        double gg =  x * -0.9689 / 100 + y *  1.8758 / 100 + z *  0.0415 / 100;
        double bb =  x *  0.0557 / 100 + y * -0.2040 / 100 + z *  1.0570 / 100;
        return String.format("#%02X%02X%02X",
                clamp8(linearToSrgb(rr) * 255),
                clamp8(linearToSrgb(gg) * 255),
                clamp8(linearToSrgb(bb) * 255));
    }

    private static double refMul(double t) {
        double t3 = t * t * t;
        return t3 > 0.008856 ? t3 : (t - 16.0 / 116.0) / 7.787;
    }

    private static double linearToSrgb(double v) {
        return v > 0.0031308 ? 1.055 * Math.pow(v, 1.0 / 2.4) - 0.055 : 12.92 * v;
    }

    private static int clamp8(double v) {
        return (int) Math.min(255, Math.max(0, Math.round(v)));
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
