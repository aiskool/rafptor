package com.rafptor.parser.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-PTX accounting of PTOCA control sequences. Tracks every opcode
 * encountered so the audit CLI can flag any non-implemented opcode.
 */
public final class PtocaByteAccountant {

    public enum OpStatus { USED, IGNORED, UNKNOWN }

    public record OpRecord(int offset,
                           int length,
                           int opcodeRaw,
                           int opcodeMasked,
                           String mnemonic,
                           OpStatus status) {

        public OpRecord {
            if (mnemonic == null) mnemonic = "";
            if (status == null) status = OpStatus.UNKNOWN;
        }
    }

    private final List<OpRecord> records = new ArrayList<>();
    private int totalPtxBytes;

    public void setTotalPtxBytes(int totalPtxBytes) {
        this.totalPtxBytes = Math.max(0, totalPtxBytes);
    }

    public int totalPtxBytes() { return totalPtxBytes; }

    public void register(int offset, int length, int opcodeRaw,
                         String mnemonic, OpStatus status) {
        records.add(new OpRecord(offset, length, opcodeRaw, opcodeRaw & 0xFE, mnemonic, status));
    }

    public int size() { return records.size(); }

    public List<OpRecord> records() { return Collections.unmodifiableList(records); }

    public PtocaOpcodeReport generateReport() {
        int used = 0, ignored = 0, unknown = 0;
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        List<OpRecord> unknownList = new ArrayList<>();
        for (OpRecord r : records) {
            counts.merge(r.opcodeMasked, 1, Integer::sum);
            switch (r.status) {
                case USED -> used += r.length;
                case IGNORED -> ignored += r.length;
                case UNKNOWN -> {
                    unknown += r.length;
                    unknownList.add(r);
                }
            }
        }
        double coverage = totalPtxBytes == 0
                ? 100.0
                : (used + ignored) * 100.0 / totalPtxBytes;
        return new PtocaOpcodeReport(
                totalPtxBytes, used, ignored, unknown,
                coverage,
                Collections.unmodifiableMap(counts),
                Collections.unmodifiableList(unknownList),
                Collections.unmodifiableList(new ArrayList<>(records)));
    }
}
