package com.rafptor.parser.modca;

import com.rafptor.parser.AfpTestFileGenerator;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TagLogicalElementTest {

    @Test
    void extracts_attribute_name_and_value() {
        byte[] payload = AfpTestFileGenerator.tlePayload("ClientId", "ACME001");
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xA0, 0x90), 0, payload);
        TagLogicalElement tle = TagLogicalElement.parse(raw);
        assertEquals("ClientId", tle.attributeName());
        assertEquals("ACME001", tle.attributeValue());
        assertNotNull(tle.triplets());
        assertEquals(2, tle.triplets().size());
    }
}
