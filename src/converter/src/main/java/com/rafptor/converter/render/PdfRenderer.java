package com.rafptor.converter.render;

import com.rafptor.converter.ConversionConfig;
import com.rafptor.converter.ir.IrDocument;
import com.rafptor.converter.ir.IrElement;
import com.rafptor.converter.ir.IrGraphic;
import com.rafptor.converter.ir.IrImage;
import com.rafptor.converter.ir.IrOverlay;
import com.rafptor.converter.ir.IrPage;
import com.rafptor.converter.ir.IrTextBlock;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Renders an {@link IrDocument} to a PDF byte stream via PDFBox.
 *
 * <p>Coordinate convention: IR positions are AFP-style with Y growing downward
 * from the page top-left corner. PDF expects Y growing upward from the bottom-left.
 * The renderer flips Y with {@code pageHeight - irY} at draw time.
 */
public final class PdfRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(PdfRenderer.class);

    private final FontLoader fontLoader;
    private final MetadataRenderer metadataRenderer = new MetadataRenderer();

    public PdfRenderer(ConversionConfig config) {
        this.fontLoader = new FontLoader(config.fontResourcesDir());
    }

    public List<String> render(IrDocument ir, OutputStream output, ConversionConfig config) throws IOException {
        List<String> warnings = new ArrayList<>();
        try (PDDocument pdfDoc = new PDDocument()) {
            for (IrPage irPage : ir.pages()) {
                PDPage pdfPage = new PDPage(new PDRectangle(
                        (float) irPage.widthPt(),
                        (float) irPage.heightPt()));
                pdfDoc.addPage(pdfPage);
                try (PDPageContentStream cs = new PDPageContentStream(pdfDoc, pdfPage)) {
                    List<IrElement> sorted = irPage.elements().stream()
                            .sorted(Comparator.comparingInt(IrElement::zOrder))
                            .toList();
                    for (IrElement e : sorted) {
                        renderElement(pdfDoc, cs, e, irPage.heightPt(), warnings);
                    }
                }
            }
            metadataRenderer.apply(pdfDoc, ir.metadata(), config);
            if (config.isPdfA()) {
                warnings.addAll(PdfACompliance.apply(pdfDoc, config));
            }
            pdfDoc.save(output);
            LOG.info("rendered pages={} warnings={}", ir.pages().size(), warnings.size());
        }
        return warnings;
    }

    private void renderElement(PDDocument doc, PDPageContentStream cs, IrElement e,
                               double pageHeight, List<String> warnings) throws IOException {
        double pdfY = pageHeight - e.y();
        if (e instanceof IrTextBlock t) {
            renderText(doc, cs, t, pdfY);
        } else if (e instanceof IrImage img) {
            renderImage(doc, cs, img, pdfY, warnings);
        } else if (e instanceof IrGraphic g) {
            renderGraphic(cs, g, pdfY);
        } else if (e instanceof IrOverlay overlay) {
            for (IrElement inner : overlay.elements()) {
                renderElement(doc, cs, inner, pageHeight, warnings);
            }
        }
    }

    private void renderText(PDDocument doc, PDPageContentStream cs, IrTextBlock text, double pdfY)
            throws IOException {
        String rendered = sanitize(text.text());
        if (rendered.isEmpty()) {
            return;
        }
        PDFont font = fontLoader.load(doc, text.fontName());
        Color color = ColorUtil.parse(text.color());
        cs.beginText();
        cs.setNonStrokingColor(color);
        cs.setFont(font, (float) text.fontSize());
        cs.newLineAtOffset((float) text.x(), (float) (pdfY - text.fontSize()));
        if (text.charSpacing() != 0) {
            cs.setCharacterSpacing((float) text.charSpacing());
        }
        try {
            cs.showText(rendered);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // The current font lacks a glyph for some character — typically
            // a PDFBox standard-14 fallback meeting a Unicode code point
            // outside WinAnsiEncoding, or a TrueType font without a mapping
            // for a control-range char. Retry with an ASCII-only rewrite;
            // if even that fails we drop the run rather than abort the page.
            try {
                cs.showText(asciiOnly(rendered));
            } catch (IllegalArgumentException | IllegalStateException ignore) {
                // give up silently on this run
            }
        }
        cs.endText();
    }

    private static String sanitize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Drop C0 controls except tab, keep printable Unicode.
            if (c < 0x20 && c != '\t') {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String asciiOnly(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(c >= 0x20 && c < 0x7F ? c : '?');
        }
        return sb.toString();
    }

    private void renderImage(PDDocument doc, PDPageContentStream cs, IrImage img, double pdfY,
                             List<String> warnings) throws IOException {
        PDImageXObject pdfImage;
        try {
            if ("jpeg".equalsIgnoreCase(img.format()) || "jpg".equalsIgnoreCase(img.format())) {
                pdfImage = JPEGFactory.createFromByteArray(doc, img.data());
            } else {
                BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(img.data()));
                if (buffered == null) {
                    warnings.add("unsupported image format (dropped)");
                    return;
                }
                if ((long) buffered.getWidth() * (long) buffered.getHeight() > 50_000_000L) {
                    warnings.add("oversized image rejected");
                    return;
                }
                pdfImage = LosslessFactory.createFromImage(doc, buffered);
            }
        } catch (IOException e) {
            warnings.add("image decoding failed (dropped)");
            return;
        }
        float drawY = (float) (pdfY - img.height());
        cs.drawImage(pdfImage, (float) img.x(), drawY, (float) img.width(), (float) img.height());
    }

    private void renderGraphic(PDPageContentStream cs, IrGraphic g, double pdfY) throws IOException {
        cs.setStrokingColor(ColorUtil.parse(g.strokeColor()));
        if (g.fillColor() != null) {
            cs.setNonStrokingColor(ColorUtil.parse(g.fillColor()));
        }
        cs.setLineWidth((float) g.lineWidth());
        switch (g.shape()) {
            case LINE, RULE -> {
                double y2 = pdfY - (g.y2() - g.y());
                cs.moveTo((float) g.x(), (float) pdfY);
                cs.lineTo((float) g.x2(), (float) y2);
                cs.stroke();
            }
            case RECT -> {
                float x = (float) g.x();
                float y = (float) (pdfY - g.height());
                cs.addRect(x, y, (float) g.width(), (float) g.height());
                if (g.fillColor() != null) {
                    cs.fillAndStroke();
                } else {
                    cs.stroke();
                }
            }
        }
    }
}
