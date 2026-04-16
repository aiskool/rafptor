package com.rafptor.converter;

import com.rafptor.parser.model.AfpDocument;
import com.rafptor.parser.model.AfpPage;
import com.rafptor.parser.ptoca.PtocaTextRun;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RafptorConverterTest {

    @Test
    void endToEndMinimalDocument(@TempDir Path tmp) throws Exception {
        AfpDocument afp = new AfpDocument("DOC00001");
        AfpPage page = new AfpPage("PAGE0001");
        page.addTextRun(new PtocaTextRun(1, 200, 200, "HELLO"));
        afp.addPage(page);

        Path out = tmp.resolve("out.pdf");
        ConversionResult result = new RafptorConverter().convert(afp, out);

        assertTrue(Files.exists(out));
        assertTrue(result.outputSize() > 0);
        assertEquals(1, result.pageCount());
        assertNotEquals(ConversionResult.Status.FAILED, result.status());

        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            assertEquals(1, pdf.getNumberOfPages());
            assertNotNull(pdf.getDocumentInformation());
        }
    }

    @Test
    void preservesTleTagsInPdfMetadata(@TempDir Path tmp) throws Exception {
        AfpDocument afp = new AfpDocument("TLEDOC");
        afp.putTag("ClientId", "ACME001");
        AfpPage page = new AfpPage("P1");
        afp.addPage(page);

        Path out = tmp.resolve("out.pdf");
        ConversionResult result = new RafptorConverter().convert(afp, out);
        assertNotEquals(ConversionResult.Status.FAILED, result.status());

        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            PDDocumentInformation info = pdf.getDocumentInformation();
            assertEquals("ACME001", info.getCustomMetadataValue("ClientId"));
        }
    }

    @Test
    void resultIsJsonSerializable(@TempDir Path tmp) throws Exception {
        AfpDocument afp = new AfpDocument("DOC");
        afp.addPage(new AfpPage("P"));
        Path out = tmp.resolve("out.pdf");
        String json = new RafptorConverter().convert(afp, out).toJson();
        assertTrue(json.startsWith("{") && json.endsWith("}"));
        assertTrue(json.contains("\"page_count\":1"));
    }
}
