package com.rafptor.api.documents;

import com.rafptor.api.config.AuthDetails;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test: verifies the thumbnail endpoint returns a valid PNG for a known document.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImagingControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DocumentRepository documentRepository;

    private static Path testPdfPath;

    private static final String TEST_TENANT = "test-tenant";

    @BeforeAll
    static void createTestPdf() throws Exception {
        testPdfPath = Files.createTempFile("rafptor-test-", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(testPdfPath.toFile());
        }
    }

    @Test
    void thumbnailEndpointReturnsPng() throws Exception {
        Document doc = new Document();
        doc.setTenantId(TEST_TENANT);
        doc.setPdfPath(testPdfPath.toAbsolutePath().toString());
        doc.setStatus(DocumentStatus.ACCEPTED);
        doc = documentRepository.save(doc);

        UsernamePasswordAuthenticationToken auth = buildAuth(TEST_TENANT);

        MvcResult result = mvc.perform(
                        get("/api/documents/" + doc.getId() + "/thumbnail")
                                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertNotNull(body);
        assertTrue(body.length > 4, "Response must contain PNG data");

        // PNG magic bytes: 0x89 0x50 0x4E 0x47
        assertEquals((byte) 0x89, body[0], "Byte 0 must be PNG magic 0x89");
        assertEquals((byte) 0x50, body[1], "Byte 1 must be PNG magic 'P'");
        assertEquals((byte) 0x4E, body[2], "Byte 2 must be PNG magic 'N'");
        assertEquals((byte) 0x47, body[3], "Byte 3 must be PNG magic 'G'");
    }

    private static UsernamePasswordAuthenticationToken buildAuth(String tenantId) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                "test-user", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        token.setDetails(new AuthDetails(tenantId, "test-user"));
        return token;
    }
}
