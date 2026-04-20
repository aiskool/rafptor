package com.rafptor.parser.audit;

import com.rafptor.parser.AfpTestFileGenerator;
import com.rafptor.parser.RafptorParser;
import com.rafptor.parser.model.AfpDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration check: the byte accountant reaches 100% coverage on a synthetic
 * document produced by our own generator (which by construction emits only
 * SFs we know how to parse).
 */
class ParserAuditIntegrationTest {

    @Test
    void generated_afp_hits_full_coverage() {
        byte[] bytes = AfpTestFileGenerator.createMinimalDocument();
        AfpDocument doc = new RafptorParser().parse(new ByteArrayInputStream(bytes));
        ByteAccountingReport r = doc.byteAccountingReport();
        assertNotNull(r, "byte accounting report must be attached");
        assertEquals(bytes.length, r.totalFileBytes());
        // Any SF our generator emits must be classified either as USED or as
        // ENVELOPE_ONLY — never left unknown.
        assertEquals(0, r.unknownBytes(), "no unknown SF expected in generator output");
        assertTrue(r.coverage() >= 99.99, "expected 100% coverage, got " + r.coverage());
    }
}
