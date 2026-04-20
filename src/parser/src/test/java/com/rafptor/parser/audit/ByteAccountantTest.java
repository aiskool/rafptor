package com.rafptor.parser.audit;

import com.rafptor.parser.audit.ByteAccountant.SfStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ByteAccountantTest {

    @Test
    void coverage_is_100_when_everything_accounted() {
        ByteAccountant a = new ByteAccountant();
        a.setTotalFileBytes(100);
        a.register(0, 40, "D3A8A8", "BDT", SfStatus.ENVELOPE_ONLY, 0);
        a.register(40, 60, "D3EE9B", "PTX", SfStatus.PARSED_USED, 0);
        ByteAccountingReport r = a.generateReport();
        assertEquals(100.0, r.coverage(), 0.0001);
        assertEquals(60, r.usedBytes());
        assertEquals(40, r.envelopeBytes());
        assertEquals(0, r.unknownBytes());
    }

    @Test
    void coverage_drops_when_unknown_bytes_present() {
        ByteAccountant a = new ByteAccountant();
        a.setTotalFileBytes(220);
        a.register(0, 100, "D3A8A8", "BDT", SfStatus.ENVELOPE_ONLY, 0);
        // Use >100 bytes to trigger the bigIgnored threshold
        a.register(100, 120, "ABCDEF", "UNKNOWN", SfStatus.UNKNOWN, 0);
        ByteAccountingReport r = a.generateReport();
        assertTrue(r.coverage() < 100.0);
        assertEquals(120, r.unknownBytes());
        assertEquals(1, r.bigIgnored().size());
    }

    @Test
    void big_ignored_threshold_is_100_bytes() {
        ByteAccountant a = new ByteAccountant();
        a.setTotalFileBytes(1000);
        a.register(0, 50, "AABBCC", "?", SfStatus.UNKNOWN, 0);  // below threshold
        a.register(50, 200, "DDEEFF", "?", SfStatus.UNKNOWN, 0); // above threshold
        assertEquals(1, a.generateReport().bigIgnored().size());
    }

    @Test
    void markLastStatus_upgrades_recent_record() {
        ByteAccountant a = new ByteAccountant();
        a.setTotalFileBytes(50);
        a.register(0, 50, "D3EE9B", "PTX", SfStatus.PARSED_IGNORED, 0);
        a.markLastStatus(SfStatus.PARSED_USED);
        ByteAccountingReport r = a.generateReport();
        assertEquals(50, r.usedBytes());
        assertEquals(0, r.ignoredBytes());
    }

    @Test
    void empty_accountant_reports_100_pct_when_file_empty() {
        ByteAccountant a = new ByteAccountant();
        a.setTotalFileBytes(0);
        assertEquals(100.0, a.generateReport().coverage(), 0.0001);
    }
}
