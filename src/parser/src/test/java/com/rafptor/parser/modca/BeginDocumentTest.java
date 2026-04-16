package com.rafptor.parser.modca;

import com.rafptor.parser.AfpTestFileGenerator;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BeginDocumentTest {

    @Test
    void parses_document_name() {
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xA8, 0xA8), 0, AfpTestFileGenerator.namePayload("TESTDOC"));
        BeginDocument parsed = BeginDocument.parse(raw);
        assertEquals("TESTDOC", parsed.documentName());
    }

    @Test
    void accepts_empty_name() {
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xA8, 0xA8), 0, new byte[0]);
        BeginDocument parsed = BeginDocument.parse(raw);
        assertEquals("", parsed.documentName());
    }
}
