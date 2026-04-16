package com.rafptor.converter.transform;

import com.rafptor.converter.ir.IrMetadata;
import com.rafptor.parser.model.AfpDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetadataExtractorTest {

    @Test
    void extractsNameAndTags() {
        AfpDocument doc = new AfpDocument("DOC00001");
        doc.putTag("ClientId", "ACME001");
        doc.putTag("AccountId", "42");
        IrMetadata m = new MetadataExtractor().extract(doc);
        assertEquals("DOC00001", m.documentName());
        assertEquals("ACME001", m.tags().get("ClientId"));
        assertEquals("42", m.tags().get("AccountId"));
    }
}
