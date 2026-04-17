package com.rafptor.converter;

import com.rafptor.parser.RafptorParser;
import com.rafptor.parser.model.AfpDocument;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class E2EConvertTest {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: E2EConvertTest <afp-file> <output-pdf>");
            System.exit(1);
        }
        Path afp = Path.of(args[0]);
        Path pdf = Path.of(args[1]);
        Files.createDirectories(pdf.getParent() == null ? Path.of(".") : pdf.getParent());

        System.out.println("=== E2E Convert: " + afp.getFileName() + " -> " + pdf.getFileName() + " ===");

        AfpDocument doc;
        try (InputStream in = Files.newInputStream(afp)) {
            doc = new RafptorParser().parse(in);
        }
        System.out.println("  Parsed: " + doc.name() + " (" + doc.pages().size() + " pages)");

        ConversionConfig cfg = ConversionConfig.builder().pdfA(false).build();
        ConversionResult result = new RafptorConverter(cfg).convert(doc, pdf);

        System.out.println("  Status:    " + result.status());
        System.out.println("  Pages:     " + result.pageCount());
        System.out.println("  Size:      " + result.outputSize() + " bytes");
        System.out.println("  Duration:  " + result.durationMs() + " ms");
        if (!result.warnings().isEmpty()) {
            System.out.println("  Warnings (" + result.warnings().size() + "):");
            for (String w : result.warnings()) {
                System.out.println("    - " + w);
            }
        }
        if (result.status() == ConversionResult.Status.FAILED) {
            System.err.println("  Error: " + result.error());
            System.exit(2);
        }
        System.out.println("  SUCCESS");
    }
}
