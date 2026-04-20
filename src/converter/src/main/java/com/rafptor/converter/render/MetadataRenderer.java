package com.rafptor.converter.render;

import com.rafptor.converter.ConversionConfig;
import com.rafptor.converter.ir.IrMetadata;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.xml.XmpSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Map;

/**
 * Writes IR metadata into the PDF DocumentInformation dictionary.
 * TLE tags become custom metadata values.
 */
public final class MetadataRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataRenderer.class);

    public void apply(PDDocument pdfDoc, IrMetadata metadata, ConversionConfig config) {
        PDDocumentInformation info = pdfDoc.getDocumentInformation();
        if (info == null) {
            info = new PDDocumentInformation();
            pdfDoc.setDocumentInformation(info);
        }
        if (config.documentTitle() != null) {
            info.setTitle(config.documentTitle());
        }
        if (config.creator() != null) {
            info.setCreator(config.creator());
            info.setProducer(config.creator());
        }
        info.setCreationDate(Calendar.getInstance());
        info.setModificationDate(Calendar.getInstance());
        if (metadata != null && metadata.tags() != null) {
            for (Map.Entry<String, String> entry : metadata.tags().entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                info.setCustomMetadataValue(entry.getKey(), entry.getValue());
            }
        }
        attachXmp(pdfDoc, metadata, config);
    }

    /**
     * Phase 6 — publish an XMP stream on the document catalog. PDF/A
     * conformance requires XMP, and downstream search tools (Adobe Bridge,
     * Windows Explorer) display XMP fields more reliably than
     * DocumentInformation custom keys.
     */
    private void attachXmp(PDDocument pdfDoc, IrMetadata metadata, ConversionConfig config) {
        try {
            XMPMetadata xmp = XMPMetadata.createXMPMetadata();
            DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
            if (config.documentTitle() != null) {
                dc.setTitle(config.documentTitle());
            }
            if (config.creator() != null) {
                dc.addCreator(config.creator());
            }
            XMPBasicSchema basic = xmp.createAndAddXMPBasicSchema();
            basic.setCreatorTool("Rafptor Converter");
            basic.setCreateDate(Calendar.getInstance());
            basic.setModifyDate(Calendar.getInstance());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            new XmpSerializer().serialize(xmp, buf, true);
            PDMetadata meta = new PDMetadata(pdfDoc);
            meta.importXMPMetadata(buf.toByteArray());
            pdfDoc.getDocumentCatalog().setMetadata(meta);
        } catch (java.io.IOException | javax.xml.transform.TransformerException e) {
            LOG.warn("failed to attach XMP metadata: {}", e.getClass().getSimpleName());
        }
    }
}
