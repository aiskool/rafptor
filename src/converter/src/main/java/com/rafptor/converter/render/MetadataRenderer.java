package com.rafptor.converter.render;

import com.rafptor.converter.ConversionConfig;
import com.rafptor.converter.ir.IrMetadata;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.util.Calendar;
import java.util.Map;

/**
 * Writes IR metadata into the PDF DocumentInformation dictionary.
 * TLE tags become custom metadata values.
 */
public final class MetadataRenderer {

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
    }
}
