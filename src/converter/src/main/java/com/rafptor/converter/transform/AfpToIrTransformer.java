package com.rafptor.converter.transform;

import com.rafptor.converter.ConversionConfig;
import com.rafptor.converter.font.FontMapper;
import com.rafptor.converter.font.StandardFontMapper;
import com.rafptor.converter.ir.IrDocument;
import com.rafptor.converter.ir.IrPage;
import com.rafptor.converter.ir.IrTextBlock;
import com.rafptor.parser.model.AfpDocument;
import com.rafptor.parser.model.AfpPage;
import com.rafptor.parser.model.PageGeometry;

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
            for (IrTextBlock block : textTransformer.transform(
                    page.textRuns(), irPage, page.fontAssignments())) {
                irPage.add(block);
            }
            imageTransformer.transform(page.images(), irPage).forEach(irPage::add);
            warnings.addAll(imageTransformer.warnings());
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
}
