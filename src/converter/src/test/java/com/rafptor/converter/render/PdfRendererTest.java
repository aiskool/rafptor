package com.rafptor.converter.render;

import com.rafptor.converter.ConversionConfig;
import com.rafptor.converter.ir.IrDocument;
import com.rafptor.converter.ir.IrPage;
import com.rafptor.converter.ir.IrTextBlock;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfRendererTest {

    @Test
    void rendersMinimalTextPage(@TempDir Path tmp) throws Exception {
        ConversionConfig cfg = ConversionConfig.defaults();
        IrDocument ir = new IrDocument("TEST");
        IrPage page = new IrPage("p", cfg.defaultPageWidthPt(), cfg.defaultPageHeightPt(), cfg.afpResolution());
        page.add(new IrTextBlock(50, 100, 0, "HELLO", "Liberation Mono", 12, 0, null));
        ir.add(page);

        Path out = tmp.resolve("out.pdf");
        List<String> warnings;
        try (OutputStream os = Files.newOutputStream(out)) {
            warnings = new PdfRenderer(cfg).render(ir, os, cfg);
        }
        assertTrue(Files.size(out) > 0);
        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            assertEquals(1, pdf.getNumberOfPages());
        }
        assertTrue(warnings != null);
    }

    @Test
    void pdfATriggersWarningWhenIccMissing(@TempDir Path tmp) throws Exception {
        ConversionConfig cfg = ConversionConfig.builder().pdfA(true).build();
        IrDocument ir = new IrDocument("TEST");
        IrPage page = new IrPage("p", cfg.defaultPageWidthPt(), cfg.defaultPageHeightPt(), cfg.afpResolution());
        page.add(new IrTextBlock(50, 100, 0, "HI", "Helvetica", 12, 0, null));
        ir.add(page);

        Path out = tmp.resolve("out.pdf");
        List<String> warnings;
        try (OutputStream os = Files.newOutputStream(out)) {
            warnings = new PdfRenderer(cfg).render(ir, os, cfg);
        }
        assertTrue(
                warnings.stream().anyMatch(w -> w.contains("ICC") || w.contains("PDF/A")),
                "expected a warning about PDF/A or ICC");
    }
}
