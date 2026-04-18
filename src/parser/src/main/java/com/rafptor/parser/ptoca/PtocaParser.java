package com.rafptor.parser.ptoca;

import com.rafptor.parser.modca.PresentationTextData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

    public List<PtocaTextRun> parse(PresentationTextData ptx) {
        if (ptx == null) {
            throw new IllegalArgumentException("ptx must not be null");
        }
        return parseBytes(ptx.payload());
    }

    public List<PtocaTextRun> parseBytes(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        List<PtocaTextRun> runs = new ArrayList<>();
        int localFontId = 0;
        int baseline = 0;
        int inline = 0;

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
                    }
                }
                case PtocaControlCode.ABSOLUTE_MOVE_BASELINE -> baseline = readUnsignedShort(data, payloadOffset, sequenceEnd);
                case PtocaControlCode.ABSOLUTE_MOVE_INLINE -> inline = readUnsignedShort(data, payloadOffset, sequenceEnd);
                case PtocaControlCode.RELATIVE_MOVE_INLINE -> inline += readUnsignedShort(data, payloadOffset, sequenceEnd);
                case PtocaControlCode.TRANSPARENT_DATA -> {
                    int textLen = sequenceEnd - payloadOffset;
                    if (textLen > 0) {
                        String text = decoder.decode(data, payloadOffset, textLen);
                        runs.add(new PtocaTextRun(localFontId, baseline, inline, text));
                    }
                }
                case PtocaControlCode.DRAW_I_AXIS_RULE, PtocaControlCode.DRAW_B_AXIS_RULE -> {
                    // geometry primitive, not modelled in this release
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
            LOG.debug("ptoca sequences={} runs={}", sequenceCount, runs.size());
        }
        return runs;
    }

    private static int readUnsignedShort(byte[] data, int offset, int end) {
        if (offset + 1 >= end) {
            return 0;
        }
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }
}
