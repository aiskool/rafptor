package com.rafptor.parser.modca;

import com.rafptor.parser.AfpTestFileGenerator;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Envelope Structured Fields (BPG/EPG/BAG/EAG/BRG/ERG/BOG/EOG/EDT) share the "name parameter" shape. */
class EnvelopeFieldsTest {

    @Test
    void begin_page_decodes_name() {
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xA8, 0xAF), 0, AfpTestFileGenerator.namePayload("PAGE0001"));
        assertEquals("PAGE0001", BeginPage.parse(raw).pageName());
    }

    @Test
    void end_page_decodes_name() {
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xA9, 0xAF), 0, AfpTestFileGenerator.namePayload("PAGE0001"));
        assertEquals("PAGE0001", EndPage.parse(raw).pageName());
    }

    @Test
    void include_page_overlay_decodes_name() {
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xAF, 0xD8), 0, AfpTestFileGenerator.namePayload("OVL00001"));
        assertEquals("OVL00001", IncludePageOverlay.parse(raw).overlayName());
    }

    @Test
    void include_page_segment_decodes_name() {
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xAF, 0x5F), 0, AfpTestFileGenerator.namePayload("SEG00001"));
        assertEquals("SEG00001", IncludePageSegment.parse(raw).segmentName());
    }

    @Test
    void include_object_decodes_name() {
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xAF, 0xC3), 0, AfpTestFileGenerator.namePayload("OBJ00001"));
        assertEquals("OBJ00001", IncludeObject.parse(raw).objectName());
    }

    @Test
    void end_document_decodes_name() {
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xA9, 0xA8), 0, AfpTestFileGenerator.namePayload("DOC00001"));
        assertEquals("DOC00001", EndDocument.parse(raw).documentName());
    }

    @Test
    void nop_records_payload_length() {
        byte[] data = new byte[]{1, 2, 3, 4};
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xEE, 0xEE), 0, data);
        NoOperation nop = NoOperation.parse(raw);
        assertEquals(4, nop.payloadLength());
    }
}
