package com.rafptor.converter.validation;

import com.rafptor.converter.ir.IrDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConversionValidator {

    public ValidationReport validate(Path pdfPath, IrDocument ir, long maxOutputBytes) {
        ValidationReport.Builder b = ValidationReport.builder();
        if (!Files.exists(pdfPath)) {
            return b.error("output file is missing").build();
        }
        try {
            long size = Files.size(pdfPath);
            if (size <= 0) {
                b.error("output file is empty");
                return b.build();
            }
            if (size > maxOutputBytes) {
                b.error("output file exceeds maxOutputBytes (" + size + " > " + maxOutputBytes + ")");
                return b.build();
            }
            try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
                int pdfPages = doc.getNumberOfPages();
                int irPages = ir.pages().size();
                if (pdfPages != irPages) {
                    b.error("page count mismatch: ir=" + irPages + " pdf=" + pdfPages);
                }
            }
        } catch (IOException e) {
            b.error("I/O validation error: " + e.getClass().getSimpleName());
        }
        return b.build();
    }
}
