package com.rafptor.converter.render;

import com.rafptor.converter.ConversionConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.xml.XmpSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies PDF/A-1b (ISO 19005-1) annotations: sRGB output intent + XMP metadata.
 *
 * <p>Full PDF/A conformance also requires embedded fonts and absence of
 * transparency; those constraints are honoured by the rendering path directly
 * (no transparency primitives are used, every font is embedded).
 */
public final class PdfACompliance {

    private PdfACompliance() {
    }

    public static List<String> apply(PDDocument doc, ConversionConfig config) {
        List<String> warnings = new ArrayList<>();
        try {
            InputStream icc = resolveIcc(config);
            if (icc == null) {
                warnings.add("PDF/A requested but no ICC profile available — output is not fully PDF/A-1b compliant.");
            } else {
                try (icc) {
                    PDOutputIntent intent = new PDOutputIntent(doc, icc);
                    intent.setInfo("sRGB IEC61966-2.1");
                    intent.setOutputCondition("sRGB");
                    intent.setOutputConditionIdentifier("sRGB IEC61966-2.1");
                    intent.setRegistryName("http://www.color.org");
                    doc.getDocumentCatalog().addOutputIntent(intent);
                }
            }

            XMPMetadata xmp = XMPMetadata.createXMPMetadata();
            PDFAIdentificationSchema pdfaId = xmp.createAndAddPDFAIdentificationSchema();
            pdfaId.setPart(1);
            pdfaId.setConformance("B");

            DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
            if (config.documentTitle() != null) {
                dc.setTitle(config.documentTitle());
            }
            if (config.creator() != null) {
                dc.addCreator(config.creator());
            }

            ByteArrayOutputStream xmpBytes = new ByteArrayOutputStream();
            new XmpSerializer().serialize(xmp, xmpBytes, true);
            PDMetadata metadata = new PDMetadata(doc);
            metadata.importXMPMetadata(xmpBytes.toByteArray());
            PDDocumentCatalog catalog = doc.getDocumentCatalog();
            catalog.setMetadata(metadata);
        } catch (Exception e) {
            warnings.add("PDF/A compliance step failed: " + e.getClass().getSimpleName());
        }
        return warnings;
    }

    private static InputStream resolveIcc(ConversionConfig config) throws IOException {
        if (config.iccProfilePath() != null && Files.exists(config.iccProfilePath())) {
            return Files.newInputStream(config.iccProfilePath());
        }
        return PdfACompliance.class.getResourceAsStream("/icc/sRGB_IEC61966-2-1.icc");
    }
}
