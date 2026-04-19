package com.rafptor.converter.transform;

import com.rafptor.converter.ConversionConfig;
import com.rafptor.converter.font.FontMapper;
import com.rafptor.converter.font.StandardFontMapper;
import com.rafptor.converter.ir.IrDocument;
import com.rafptor.converter.ir.IrGraphic;
import com.rafptor.converter.ir.IrImage;
import com.rafptor.converter.ir.IrPage;
import com.rafptor.converter.ir.IrTextBlock;
import com.rafptor.parser.model.AfpDocument;
import com.rafptor.parser.model.AfpPage;
import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.PageGeometry;
import com.rafptor.parser.modca.IncludeObject;
import com.rafptor.parser.ptoca.PtocaRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Walks an {@link AfpDocument} and produces an {@link IrDocument}.
 */
public final class AfpToIrTransformer {

    private final ConversionConfig config;
    private final FontMapper fontMapper;
    private final TextTransformer textTransformer;
    private final ImageTransformer imageTransformer = new ImageTransformer();
    private final GraphicTransformer graphicTransformer = new GraphicTransformer();
    private final BarcodeTransformer barcodeTransformer = new BarcodeTransformer();
    private final OverlayResolver overlayResolver = new OverlayResolver();
    private final MetadataExtractor metadataExtractor = new MetadataExtractor();

    private final List<String> warnings = new ArrayList<>();

    public AfpToIrTransformer(ConversionConfig config) {
        this(config, new StandardFontMapper());
    }

    public AfpToIrTransformer(ConversionConfig config, FontMapper fontMapper) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        if (fontMapper == null) {
            throw new IllegalArgumentException("fontMapper must not be null");
        }
        this.config = config;
        this.fontMapper = fontMapper;
        this.textTransformer = new TextTransformer(fontMapper);
    }

    public IrDocument transform(AfpDocument afp) {
        if (afp == null) {
            throw new IllegalArgumentException("afp must not be null");
        }
        warnings.clear();
        IrDocument out = new IrDocument(afp.name());
        for (AfpPage page : afp.pages()) {
            PageGeometry geom = page.geometry();
            double widthPt = geom != null ? geom.widthPt() : config.defaultPageWidthPt();
            double heightPt = geom != null ? geom.heightPt() : config.defaultPageHeightPt();
            int xResolution = geom != null
                    ? geom.effectivePtxXResolution()
                    : config.afpResolution();
            int yResolution = geom != null
                    ? geom.effectivePtxYResolution()
                    : config.afpResolution();
            // Guard against degenerate or absent geometry values — some test
            // fixtures do not carry a PGD, and the Liberation-only PDF must
            // still stay in the printable-area ballpark.
            if (widthPt <= 1 || widthPt > 10_000) widthPt = config.defaultPageWidthPt();
            if (heightPt <= 1 || heightPt > 10_000) heightPt = config.defaultPageHeightPt();
            if (xResolution <= 0) xResolution = config.afpResolution();
            if (yResolution <= 0) yResolution = config.afpResolution();
            IrPage irPage = new IrPage(page.name(), widthPt, heightPt, xResolution, yResolution);
            // Emit PTOCA rules (DIR / DBR) FIRST so they sit behind any text
            // that overlaps them — coloured header bars behind white text,
            // gray separators behind body text.
            for (PtocaRule rule : page.rules()) {
                irPage.add(buildIrGraphicFromRule(rule, irPage));
            }
            for (IrTextBlock block : textTransformer.transform(
                    page.textRuns(), irPage, page.fontAssignments(), page.fontPointSizes())) {
                irPage.add(block);
            }
            imageTransformer.transform(page.images(), irPage).forEach(irPage::add);
            warnings.addAll(imageTransformer.warnings());
            // Resolve any Include Object references that point at an embedded
            // image (JPEG/PNG) carried at document level. This covers the
            // AFPWorld / MO:DCA-P5 pattern where a logo sits inside a BRS /
            // BFN envelope and is placed on the page via IOB.
            for (AfpStructuredField sf : page.structuredFields()) {
                if (sf instanceof IncludeObject iob) {
                    IrImage img = buildIrImageFromEmbedded(afp, iob, irPage);
                    if (img != null) {
                        irPage.add(img);
                    }
                }
            }
            graphicTransformer.transform(page.graphics(), irPage).forEach(irPage::add);
            // Graphic / barcode transformers are stubs today — see warnings.
            page.resourceReferences().stream()
                    .filter(r -> r.type() == com.rafptor.parser.model.AfpResource.ResourceType.PAGE_OVERLAY)
                    .forEach(r -> warnings.add(overlayResolver.warningFor(r)));
            if (StubPageGenerator.needsStub(irPage, page)) {
                irPage.add(StubPageGenerator.build(irPage, page));
            }
            out.add(irPage);
        }
        if (afp.pages().isEmpty()) {
            IrPage stubPage = new IrPage(
                    "STUB",
                    config.defaultPageWidthPt(),
                    config.defaultPageHeightPt(),
                    config.afpResolution());
            stubPage.add(new IrTextBlock(
                    72, 72, 0,
                    "Ce document ne contient pas de pages exploitables. "
                            + "Il peut s'agir d'une ressource partagee ou d'un fragment.",
                    "Liberation Sans", 11.0, 0.0, "#4a4a4a"));
            out.add(stubPage);
            warnings.add("AFP stream had no pages; emitted a single stub page.");
        } else if (afp.pages().stream().anyMatch(p -> !p.textRuns().isEmpty())) {
            // We've at least produced text; keep quiet about stubs in the common case.
        } else {
            warnings.add("No PTOCA text runs found in the AFP stream.");
        }
        out.setMetadata(metadataExtractor.extract(afp));
        return out;
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Translate one PTOCA rule into an {@link IrGraphic} {@code RECT} element.
     *
     * <p>Rule geometry (MO:DCA/P5 convention observed in AFPWorld, DOC1,
     * Adobe Output): the rule starts at the current baseline cursor and
     * extends <em>upward</em> from the baseline by {@code thickness} L-units
     * for an I-axis rule, or <em>rightward</em> from the inline cursor by
     * {@code thickness} L-units for a B-axis rule. That anchor convention is
     * why filled header bars appear just above the text baseline in the
     * reference output.
     */
    private IrGraphic buildIrGraphicFromRule(PtocaRule rule, IrPage page) {
        double x = page.toPointsX(rule.inlinePosition());
        double yBaseline = page.toPointsY(rule.baselinePosition());
        double length = rule.direction() == PtocaRule.Direction.I_AXIS
                ? page.toPointsX(rule.lengthLUnits())
                : page.toPointsY(rule.lengthLUnits());
        double thickness = page.toPointsY(rule.thicknessLUnits());
        if (thickness <= 0) thickness = 0.5;
        double rectX, rectY, rectW, rectH;
        if (rule.direction() == PtocaRule.Direction.I_AXIS) {
            rectX = x;
            // PTOCA rule's anchor is the baseline cursor; the rule extends
            // DOWNWARD by `thickness`. Composers position their header bars
            // so that the baseline is the top of the bar and the white
            // text runs sit inside.
            rectY = yBaseline;
            rectW = length;
            rectH = thickness;
        } else {
            rectX = x;
            rectY = yBaseline;
            rectW = thickness;
            rectH = length;
        }
        return new IrGraphic(
                rectX, rectY, 0, IrGraphic.Shape.RECT,
                rectX + rectW, rectY + rectH,
                rectW, rectH,
                0.0,                             // corner radius — sharp rect
                0.0,                             // line width — pure fill
                rule.colorHex(),                 // stroke color (unused)
                rule.colorHex(),                 // fill color
                IrGraphic.StrokePattern.SOLID);
    }

    /**
     * Build an IrImage from an IOB that references an embedded JPEG / PNG
     * the parser captured at document level. Origin and size come from the
     * IOB wire layout (XoaOset, YoaOset, XocaOset, YocaOset in L-units).
     * When a size is absent (0), fall back to the image's natural pixel
     * dimensions mapped at 96 DPI.
     */
    private IrImage buildIrImageFromEmbedded(AfpDocument afp, IncludeObject iob, IrPage page) {
        byte[] data = afp.embeddedObject(iob.objectName());
        if (data == null || data.length == 0) {
            return null;
        }
        String kind = afp.embeddedObjectKind(iob.objectName()).toLowerCase();
        String format = kind.equals("jpeg") ? "jpeg"
                : kind.equals("png") ? "png"
                : kind;
        double x = iob.xOriginLUnits() > 0 ? page.toPointsX(iob.xOriginLUnits()) : 0;
        double y = iob.yOriginLUnits() > 0 ? page.toPointsY(iob.yOriginLUnits()) : 0;
        double width = iob.xSizeLUnits() > 0 ? page.toPointsX(iob.xSizeLUnits()) : 0;
        double height = iob.ySizeLUnits() > 0 ? page.toPointsY(iob.ySizeLUnits()) : 0;
        // Fall back to the image's native pixel size at 96 dpi when the IOB
        // did not declare an explicit size.
        if (width <= 0 || height <= 0) {
            int[] dims = readImageDimensions(data, format);
            if (dims != null) {
                double nativeWPt = dims[0] * 72.0 / 96.0;
                double nativeHPt = dims[1] * 72.0 / 96.0;
                if (width <= 0) width = nativeWPt;
                if (height <= 0) height = nativeHPt;
            } else {
                // Final defensive fallback — a modest square.
                if (width <= 0) width = 60;
                if (height <= 0) height = 20;
            }
        }
        return new IrImage(x, y, 0, data, format, width, height, 96);
    }

    /**
     * Best-effort pixel-size probe for a JPEG or PNG blob without pulling in
     * an ImageIO dependency. Returns {@code null} on anything but the common
     * SOF0/SOF2 JPEG or IHDR PNG marker.
     */
    private static int[] readImageDimensions(byte[] data, String format) {
        if ("png".equals(format) && data.length >= 24
                && data[0] == (byte) 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
            int w = ((data[16] & 0xFF) << 24) | ((data[17] & 0xFF) << 16)
                    | ((data[18] & 0xFF) << 8) | (data[19] & 0xFF);
            int h = ((data[20] & 0xFF) << 24) | ((data[21] & 0xFF) << 16)
                    | ((data[22] & 0xFF) << 8) | (data[23] & 0xFF);
            return new int[]{w, h};
        }
        if ("jpeg".equals(format)) {
            int p = 2;
            while (p + 8 < data.length) {
                if ((data[p] & 0xFF) != 0xFF) { p++; continue; }
                int marker = data[p + 1] & 0xFF;
                if (marker == 0xD8 || marker == 0xD9) { p += 2; continue; }
                if (marker == 0xDA) break;
                int segLen = ((data[p + 2] & 0xFF) << 8) | (data[p + 3] & 0xFF);
                if (marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                    int h = ((data[p + 5] & 0xFF) << 8) | (data[p + 6] & 0xFF);
                    int w = ((data[p + 7] & 0xFF) << 8) | (data[p + 8] & 0xFF);
                    return new int[]{w, h};
                }
                p += 2 + segLen;
            }
        }
        return null;
    }
}
