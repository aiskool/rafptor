package com.rafptor.parser.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SfRegistryTest {

    @Test
    void lookup_resolves_core_document_ids() {
        assertEquals("BDT", SfRegistry.lookup("D3A8A8").orElseThrow().mnemonic());
        assertEquals("EDT", SfRegistry.lookup("D3A9A8").orElseThrow().mnemonic());
        assertEquals("PTX", SfRegistry.lookup("D3EE9B").orElseThrow().mnemonic());
    }

    @Test
    void lookup_is_case_insensitive() {
        assertTrue(SfRegistry.lookup("d3a8a8").isPresent());
        assertTrue(SfRegistry.lookup("D3a8A8").isPresent());
    }

    @Test
    void unknown_id_returns_empty() {
        assertTrue(SfRegistry.lookup("ABCDEF").isEmpty());
    }

    @Test
    void isImplemented_reflects_rafptor_coverage() {
        // PTX is implemented end-to-end.
        assertTrue(SfRegistry.isImplemented("D3EE9B"));
        // FND is in spec but Rafptor does not parse it yet.
        assertFalse(SfRegistry.isImplemented("D3A689"));
    }

    @Test
    void registry_has_broad_coverage() {
        // Sanity check — we want significantly more than a placeholder.
        assertTrue(SfRegistry.size() >= 50,
                "SfRegistry should cover the MO:DCA spec broadly; got " + SfRegistry.size());
    }
}
