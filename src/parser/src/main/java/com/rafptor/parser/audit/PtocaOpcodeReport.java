package com.rafptor.parser.audit;

import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of PTOCA opcode coverage for one PTX block.
 */
public record PtocaOpcodeReport(int totalPtxBytes,
                                int usedBytes,
                                int ignoredBytes,
                                int unknownBytes,
                                double coverage,
                                Map<Integer, Integer> opcodeCounts,
                                List<PtocaByteAccountant.OpRecord> unknownOpcodes,
                                List<PtocaByteAccountant.OpRecord> records) {

    public PtocaOpcodeReport {
        if (opcodeCounts == null) opcodeCounts = Map.of();
        if (unknownOpcodes == null) unknownOpcodes = List.of();
        if (records == null) records = List.of();
    }

    public boolean hasUnknownOpcodes() {
        return !unknownOpcodes.isEmpty();
    }
}
