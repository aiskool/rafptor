package com.rafptor.parser.audit;

import com.rafptor.parser.audit.PtocaByteAccountant.OpStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PtocaByteAccountantTest {

    @Test
    void counts_accumulate_per_opcode() {
        PtocaByteAccountant a = new PtocaByteAccountant();
        a.setTotalPtxBytes(30);
        a.register(0, 5, 0xDA, "TRN", OpStatus.USED);
        a.register(5, 5, 0xDA, "TRN", OpStatus.USED);
        a.register(10, 4, 0xD2, "AMB", OpStatus.USED);
        PtocaOpcodeReport r = a.generateReport();
        assertEquals(2, (int) r.opcodeCounts().get(0xDA));
        assertEquals(1, (int) r.opcodeCounts().get(0xD2));
        assertEquals(14, r.usedBytes());
    }

    @Test
    void unknown_opcode_listed_separately() {
        PtocaByteAccountant a = new PtocaByteAccountant();
        a.setTotalPtxBytes(20);
        a.register(0, 10, 0xDA, "TRN", OpStatus.USED);
        a.register(10, 10, 0xAA, "UNKNOWN", OpStatus.UNKNOWN);
        PtocaOpcodeReport r = a.generateReport();
        assertTrue(r.hasUnknownOpcodes());
        assertEquals(1, r.unknownOpcodes().size());
        assertEquals(0xAA, r.unknownOpcodes().get(0).opcodeRaw());
    }

    @Test
    void coverage_percent() {
        PtocaByteAccountant a = new PtocaByteAccountant();
        a.setTotalPtxBytes(100);
        a.register(0, 60, 0xDA, "TRN", OpStatus.USED);
        a.register(60, 40, 0xAA, "UNKNOWN", OpStatus.UNKNOWN);
        assertEquals(60.0, a.generateReport().coverage(), 0.0001);
    }
}
