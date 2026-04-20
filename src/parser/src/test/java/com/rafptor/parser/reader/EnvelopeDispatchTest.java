package com.rafptor.parser.reader;

import com.rafptor.parser.audit.SfInfo;
import com.rafptor.parser.audit.SfRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1: every envelope-like entry in SfRegistry must now resolve via the
 * dispatcher. This guards against the "silent UnknownStructuredField" drift
 * that caused earlier bugs.
 */
class EnvelopeDispatchTest {

    @Test
    void every_registered_begin_or_end_sf_is_dispatched() {
        for (var e : SfRegistry.entries().entrySet()) {
            SfInfo info = e.getValue();
            String mn = info.mnemonic();
            // Only enforce for SFs whose mnemonic starts with B or E — those
            // are structural envelope begin/end pairs.
            if ((mn.startsWith("B") || mn.startsWith("E")) && info.implemented()) {
                assertTrue(StructuredFieldReader.isDispatchable(e.getKey()),
                        "envelope SF not dispatchable: " + mn + " (" + e.getKey() + ")");
            }
        }
    }
}
