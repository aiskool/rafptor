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
        // "Liberation Sans Bold" → base "LiberationSans", suffix "Bold".
        // "Liberation Sans"      → base "LiberationSans", suffix "".
        // Try multiple conventional filename shapes; the Liberation TTFs ship
        // as "LiberationSans-Bold.ttf", "LiberationSerif-Bold.ttf", etc.
        String[] tokens = name.trim().split("\\s+");
        String baseNoSpaces = name.replace(" ", "");
        String family;
        StringBuilder suffix = new StringBuilder();
        if (tokens.length >= 2) {
            // Heuristic: first two tokens are family, remainder is the weight
            // or style ("Bold", "Italic", "Bold Italic").
            family = tokens[0] + tokens[1];
            for (int i = 2; i < tokens.length; i++) {
                suffix.append(tokens[i]);
            }
        } else {
            family = baseNoSpaces;
        }
        String suf = suffix.toString();
        String[] candidates;
        if (suf.isEmpty()) {
            candidates = new String[]{
                    baseNoSpaces + ".ttf",
                    baseNoSpaces + "-Regular.ttf",
                    baseNoSpaces + "Regular.ttf",
                    family + "-Regular.ttf",
                    family + ".ttf"
            };
        } else {
            candidates = new String[]{
                    family + "-" + suf + ".ttf",
                    family + suf + ".ttf",
                    baseNoSpaces + ".ttf",
                    baseNoSpaces + "-Regular.ttf"
            };
        }
        for (String fileName : candidates) {
            InputStream in = FontLoader.class.getResourceAsStream("/fonts/" + fileName);
            if (in != null) {
                try (InputStream stream = in) {
                    return org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, stream);
                } catch (Exception ignore) {
                    // try next candidate
                }
            }
            if (fontDir != null) {
                Path onDisk = fontDir.resolve(fileName);
                if (Files.exists(onDisk)) {
                    try (InputStream stream = Files.newInputStream(onDisk)) {
                        return org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, stream);
                    } catch (Exception ignore) {
                        // try next
                    }
                }
            }
        }
        return null;
    }

    private PDFont fallback(String name) {
        String lower = name.toLowerCase();
        boolean bold = lower.contains("bold") || lower.contains("black")
                || lower.contains("heavy") || lower.contains("fett");
        boolean italic = lower.contains("italic") || lower.contains("oblique")
                || lower.contains("kursiv");
        if (lower.contains("mono") || lower.contains("courier")) {
            if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE);
            if (bold) return new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
            if (italic) return new PDType1Font(Standard14Fonts.FontName.COURIER_OBLIQUE);
            return new PDType1Font(Standard14Fonts.FontName.COURIER);
        }
        if (lower.contains("serif") || lower.contains("times") || lower.contains("roman")) {
            if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
            if (bold) return new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            if (italic) return new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC);
            return new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
        }
        if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
        if (bold) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        if (italic) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }
}
