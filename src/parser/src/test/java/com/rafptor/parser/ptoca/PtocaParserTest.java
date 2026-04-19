package com.rafptor.parser.ptoca;

import com.rafptor.parser.AfpTestFileGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PtocaParserTest {

    @Test
    void extracts_hello_text() {
        byte[] payload = AfpTestFileGenerator.ptocaPayload("HELLO");
        PtocaParser parser = new PtocaParser();
        List<PtocaTextRun> runs = parser.parseBytes(payload);
        assertEquals(1, runs.size());
        PtocaTextRun run = runs.get(0);
        assertEquals("HELLO", run.text());
        assertEquals(1, run.localFontId());
        assertEquals(0, run.baselinePosition());
        assertEquals(0, run.inlinePosition());
    }

    @Test
    void tracks_absolute_moves() {
        PtocaParser parser = new PtocaParser();
        byte[] scfl = {0x03, (byte) 0xF1, 0x01};
        byte[] amb = {0x04, (byte) 0xD2, 0x01, 0x02};
        byte[] ami = {0x04, (byte) 0xC6, 0x03, 0x04};
        byte[] trn = {0x04, (byte) 0xDA, (byte) 0xC8, (byte) 0xC9};
        byte[] data = concat(scfl, amb, ami, trn);
        List<PtocaTextRun> runs = parser.parseBytes(data);
        assertEquals(1, runs.size());
        assertEquals(0x0102, runs.get(0).baselinePosition());
        assertEquals(0x0304, runs.get(0).inlinePosition());
        assertEquals("HI", runs.get(0).text());
    }

    @Test
    void relative_move_accumulates() {
        PtocaParser parser = new PtocaParser();
        byte[] rmi1 = {0x04, (byte) 0xC8, 0x00, 0x10};
        byte[] rmi2 = {0x04, (byte) 0xC8, 0x00, 0x20};
        byte[] trn = {0x03, (byte) 0xDA, (byte) 0xC1};
        byte[] data = concat(rmi1, rmi2, trn);
        List<PtocaTextRun> runs = parser.parseBytes(data);
        assertEquals(1, runs.size());
        assertEquals(0x30, runs.get(0).inlinePosition());
    }

    @Test
    void skips_unknown_sequences_tolerantly() {
        PtocaParser parser = new PtocaParser();
        byte[] unknown = {0x04, (byte) 0x7F, 0x00, 0x00};
        byte[] trn = {0x03, (byte) 0xDA, (byte) 0xC1};
        List<PtocaTextRun> runs = parser.parseBytes(concat(unknown, trn));
        assertEquals(1, runs.size());
        assertEquals("A", runs.get(0).text());
    }

    @Test
    void empty_payload_returns_no_runs() {
        List<PtocaTextRun> runs = new PtocaParser().parseBytes(new byte[0]);
        assertTrue(runs.isEmpty());
    }

    @Test
    void decodes_french_ebcdic_1147() {
        PtocaParser parser = new PtocaParser(new EbcdicDecoder("IBM1147"));
        byte[] text = "ÉTÉ".getBytes(java.nio.charset.Charset.forName("IBM1147"));
        byte[] trn = new byte[2 + text.length];
        trn[0] = (byte) (2 + text.length);
        trn[1] = (byte) 0xDA;
        System.arraycopy(text, 0, trn, 2, text.length);
        List<PtocaTextRun> runs = parser.parseBytes(trn);
        assertEquals(1, runs.size());
        assertEquals("ÉTÉ", runs.get(0).text());
    }

    @Test
    void per_local_id_code_page_selects_decoder() {
        // Two runs on two distinct local font ids, each with its own code page.
        // Bytes that look identical in IBM037 vs IBM500 would hide the bug, so
        // use code points that differ: U+00A2 (¢) is 0x4A in IBM037 and 0xB0 in
        // IBM500. We encode the same word in both charsets and assert that the
        // right charset was used for each run.
        byte[] en = "HELLO".getBytes(java.nio.charset.Charset.forName("IBM037"));
        byte[] fr = "SANTÉ".getBytes(java.nio.charset.Charset.forName("IBM500"));
        byte[] scfl1 = {0x03, (byte) 0xF1, 0x01};
        byte[] trn1 = prefixTrn(en);
        byte[] scfl2 = {0x03, (byte) 0xF1, 0x02};
        byte[] trn2 = prefixTrn(fr);
        byte[] data = concat(scfl1, trn1, scfl2, trn2);

        PtocaParser parser = new PtocaParser();
        Map<Integer, String> codePages = Map.of(1, "T1V10037", 2, "T1V10500");
        List<PtocaTextRun> runs = parser.parseBytes(data, codePages);
        assertEquals(2, runs.size());
        assertEquals("HELLO", runs.get(0).text());
        assertEquals(1, runs.get(0).localFontId());
        assertEquals("SANTÉ", runs.get(1).text());
        assertEquals(2, runs.get(1).localFontId());
    }

    @Test
    void detects_utf16_be_trn_payload() {
        // MO:DCA/P5 streams produced by DOC1 / Adobe Output / Compart emit
        // Unicode code points inside TRN when the coded font is a TrueType
        // resource mapped through MDR. The decoder must auto-detect that
        // shape rather than force EBCDIC conversion.
        byte[] utf16 = "Hello".getBytes(java.nio.charset.StandardCharsets.UTF_16BE);
        byte[] trn = new byte[2 + utf16.length];
        trn[0] = (byte) (2 + utf16.length);
        trn[1] = (byte) 0xDA;
        System.arraycopy(utf16, 0, trn, 2, utf16.length);
        PtocaParser parser = new PtocaParser();
        List<PtocaTextRun> runs = parser.parseBytes(trn);
        assertEquals(1, runs.size());
        assertEquals("Hello", runs.get(0).text());
    }

    @Test
    void unknown_code_page_falls_back_to_ibm500() {
        byte[] text = "OK".getBytes(java.nio.charset.Charset.forName("IBM500"));
        byte[] scfl = {0x03, (byte) 0xF1, 0x07};
        byte[] trn = prefixTrn(text);
        PtocaParser parser = new PtocaParser();
        List<PtocaTextRun> runs = parser.parseBytes(concat(scfl, trn), Map.of(7, "T1BOGUS9"));
        assertEquals(1, runs.size());
        assertEquals("OK", runs.get(0).text());
    }

    private static byte[] prefixTrn(byte[] text) {
        byte[] trn = new byte[2 + text.length];
        trn[0] = (byte) (2 + text.length);
        trn[1] = (byte) 0xDA;
        System.arraycopy(text, 0, trn, 2, text.length);
        return trn;
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) {
            total += p.length;
        }
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }
}
