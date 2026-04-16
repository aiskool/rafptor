package com.rafptor.converter.render;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves a logical font name (as stored in IrTextBlock) to a concrete PDFont
 * the renderer can use. Attempts to load the TrueType file from the runtime
 * classpath first, then falls back to a PDFBox standard-14 type-1 font.
 */
public final class FontLoader {

    private final Path fontDir;
    private final Map<String, PDFont> cache = new HashMap<>();

    public FontLoader(Path fontDir) {
        this.fontDir = fontDir;
    }

    public PDFont load(PDDocument document, String logicalName) {
        PDFont cached = cache.get(logicalName);
        if (cached != null) {
            return cached;
        }
        PDFont resolved = tryLoadTrueType(document, logicalName);
        if (resolved == null) {
            resolved = fallback(logicalName);
        }
        cache.put(logicalName, resolved);
        return resolved;
    }

    private PDFont tryLoadTrueType(PDDocument document, String name) {
        String fileName = name.replace(" ", "") + ".ttf";
        InputStream in = FontLoader.class.getResourceAsStream("/fonts/" + fileName);
        if (in != null) {
            try (InputStream stream = in) {
                return org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, stream);
            } catch (Exception ignore) {
                // fall through to disk lookup
            }
        }
        if (fontDir != null) {
            Path onDisk = fontDir.resolve(fileName);
            if (Files.exists(onDisk)) {
                try (InputStream stream = Files.newInputStream(onDisk)) {
                    return org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, stream);
                } catch (Exception ignore) {
                    // fall through to standard-14
                }
            }
        }
        return null;
    }

    private PDFont fallback(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("mono") || lower.contains("courier")) {
            return new PDType1Font(Standard14Fonts.FontName.COURIER);
        }
        if (lower.contains("serif") || lower.contains("times")) {
            return new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }
}
