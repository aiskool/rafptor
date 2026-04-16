package com.rafptor.parser.reader;

import com.rafptor.parser.AfpTestFileGenerator;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import com.rafptor.parser.modca.BeginDocument;
import com.rafptor.parser.modca.UnknownStructuredField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StructuredFieldReaderTest {

    @Test
    void dispatches_known_bdt() {
        byte[] payload = AfpTestFileGenerator.namePayload("DOC00001");
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xA8, 0xA8), 0, payload);
        var parsed = StructuredFieldReader.dispatch(raw);
        BeginDocument bdt = assertInstanceOf(BeginDocument.class, parsed);
        assertEquals("DOC00001", bdt.documentName());
    }

    @Test
    void wraps_unknown_field() {
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xFF, 0xFF), 0, new byte[]{1, 2, 3});
        var parsed = StructuredFieldReader.dispatch(raw);
        UnknownStructuredField unk = assertInstanceOf(UnknownStructuredField.class, parsed);
        assertEquals(3, unk.dataLength());
        assertEquals(0xD3, unk.id().classByte());
    }

    @Test
    void preserves_field_id() {
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xEE, 0xEE), 0, new byte[]{});
        var parsed = StructuredFieldReader.dispatch(raw);
        assertEquals(0xEE, parsed.id().categoryByte());
    }
}
