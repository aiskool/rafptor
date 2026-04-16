package com.rafptor.parser;

import com.rafptor.parser.exception.AfpParseException;
import com.rafptor.parser.model.AfpDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RafptorParserTest {

    @Test
    void parses_minimal_document_end_to_end() {
        byte[] stream = AfpTestFileGenerator.createMinimalDocument();
        AfpDocument doc = new RafptorParser().parse(new ByteArrayInputStream(stream));
        assertEquals("DOC00001", doc.name());
        assertEquals(1, doc.pages().size());
        assertEquals("PAGE0001", doc.pages().get(0).name());
        assertEquals(1, doc.pages().get(0).textRuns().size());
        assertEquals("HELLO", doc.pages().get(0).textRuns().get(0).text());
        assertTrue(doc.resourceReferences().stream()
                .anyMatch(r -> r.name().equals("FONTBOLD")));
    }

    @Test
    void parses_multipage_document() {
        byte[] stream = AfpTestFileGenerator.createMultipageDocument(5);
        AfpDocument doc = new RafptorParser().parse(new ByteArrayInputStream(stream));
        assertEquals("MULTIDOC", doc.name());
        assertEquals(5, doc.pages().size());
        assertEquals("PAGE0003", doc.pages().get(2).name());
    }

    @Test
    void collects_tle_tags() {
        byte[] stream = AfpTestFileGenerator.createDocumentWithTle(Map.of(
                "ClientId", "ACME001",
                "AccountId", "123"));
        AfpDocument doc = new RafptorParser().parse(new ByteArrayInputStream(stream));
        assertEquals("ACME001", doc.tags().get("ClientId"));
        assertEquals("123", doc.tags().get("AccountId"));
    }

    @Test
    void rejects_malformed_stream() {
        byte[] bad = AfpTestFileGenerator.createStreamMissingCarriageControl();
        assertThrows(AfpParseException.class,
                () -> new RafptorParser().parse(new ByteArrayInputStream(bad)));
    }
}
