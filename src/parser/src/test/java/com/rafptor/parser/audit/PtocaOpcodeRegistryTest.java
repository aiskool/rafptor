package com.rafptor.parser.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PtocaOpcodeRegistryTest {

    @Test
    void core_opcodes_are_registered_and_implemented() {
        assertEquals("TRN", PtocaOpcodeRegistry.lookup(0xDA).orElseThrow().mnemonic());
        assertEquals("AMB", PtocaOpcodeRegistry.lookup(0xD2).orElseThrow().mnemonic());
        assertEquals("DIR", PtocaOpcodeRegistry.lookup(0xE4).orElseThrow().mnemonic());
        assertEquals("SEC", PtocaOpcodeRegistry.lookup(0x80).orElseThrow().mnemonic());
        assertTrue(PtocaOpcodeRegistry.isImplemented(0xDA));
        assertTrue(PtocaOpcodeRegistry.isImplemented(0xD2));
        assertTrue(PtocaOpcodeRegistry.isImplemented(0xE4));
    }

    @Test
    void chained_low_bit_is_masked_in_lookup() {
        // 0xDB = 0xDA | 0x01 (chained TRN). Lookup must mask off the chain flag.
        assertEquals("TRN", PtocaOpcodeRegistry.lookup(0xDB).orElseThrow().mnemonic());
        assertEquals("AMB", PtocaOpcodeRegistry.lookup(0xD3).orElseThrow().mnemonic());
    }

    @Test
    void unknown_opcode_returns_empty() {
        assertTrue(PtocaOpcodeRegistry.lookup(0x10).isEmpty());
    }
}
