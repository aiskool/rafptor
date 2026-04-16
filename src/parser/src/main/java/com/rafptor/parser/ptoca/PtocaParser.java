package com.rafptor.parser.ptoca;

import com.rafptor.parser.exception.MalformedFieldException;
import com.rafptor.parser.modca.PresentationTextData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a PTOCA control-sequence stream.
 *
 * <p>PTOCA control sequences use one of two length encodings:
 * <ul>
 *   <li><b>Escape form</b>: {@code 2B D8 LL FN …} where LL is the length byte covering
 *       LL+FN+payload. Rafptor uses this form.</li>
 *   <li><b>Chained form</b>: {@code LL FN …} — implementation-dependent. Treated
 *       tolerantly here.</li>
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

    private static final int ESCAPE_1 = 0x2B;
    private static final int ESCAPE_2 = 0xD8;

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

            if (header == ESCAPE_1 && pos + 3 < data.length && (data[pos + 1] & 0xFF) == ESCAPE_2) {
                length = data[pos + 2] & 0xFF;
                opcode = data[pos + 3] & 0xFF;
                payloadOffset = pos + 4;
                if (length < 2) {
                    throw new MalformedFieldException(
                            "PTOCA escape length " + length + " < 2 at offset " + pos);
                }
            } else {
                length = header;
                if (length < 2 || pos + 1 >= data.length) {
                    throw new MalformedFieldException(
                            "PTOCA chained length " + length + " invalid at offset " + pos);
                }
                opcode = data[pos + 1] & 0xFF;
                payloadOffset = pos + 2;
            }

            int sequenceEnd = payloadOffset + (length - 2);
            if (sequenceEnd > data.length) {
                throw new MalformedFieldException(
                        "PTOCA sequence of declared length " + length
                                + " extends past payload at offset " + pos);
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
