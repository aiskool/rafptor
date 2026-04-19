package com.rafptor.converter.transform;

import com.rafptor.converter.ir.IrPage;
import com.rafptor.converter.ir.IrTextBlock;
import com.rafptor.parser.model.AfpPage;
import com.rafptor.parser.model.AfpResource;

/**
 * Builds a human-readable explanation block injected on pages that lack
 * textual content. Saves the viewer from an eerie blank PDF when the source
 * AFP only contains unresolved external references (overlays, page segments,
 * IOCA rasters we cannot decode).
 */
final class StubPageGenerator {

    /**
     * Returns {@code true} when the page should receive a stub block. A page
     * is considered empty when it produced no {@link IrTextBlock}s and no
     * {@link com.rafptor.converter.ir.IrImage}s either.
     */
    static boolean needsStub(IrPage irPage, AfpPage sourcePage) {
        if (irPage == null) return false;
        boolean anyContent = irPage.elements().stream().anyMatch(e ->
                e instanceof IrTextBlock || e instanceof com.rafptor.converter.ir.IrImage);
        return !anyContent && sourcePage != null;
    }

    static IrTextBlock build(IrPage irPage, AfpPage sourcePage) {
        int overlays = countByType(sourcePage, AfpResource.ResourceType.PAGE_OVERLAY);
        int segments = countByType(sourcePage, AfpResource.ResourceType.PAGE_SEGMENT);
        int containers = countByType(sourcePage, AfpResource.ResourceType.OBJECT_CONTAINER);
        int images = sourcePage.images().size();

        StringBuilder message = new StringBuilder();
        message.append("Ce document contient des ressources externes ");
        message.append("(en-tetes, logos) qui seront integrees lors de la ");
        message.append("connexion a votre systeme.");

        if (overlays + segments + containers + images > 0) {
            message.append("  Detail detecte: ");
            boolean first = true;
            first = appendCounter(message, first, overlays, "modele de page", "modeles de page");
            first = appendCounter(message, first, segments, "segment de page", "segments de page");
            first = appendCounter(message, first, containers, "objet inclus", "objets inclus");
            appendCounter(message, first, images, "image", "images");
            message.append('.');
        }

        // Place near the top-left, 1 inch margin; the font ships inside the
        // converter classpath so the renderer never falls back to courier.
        double x = 72;
        double y = 72;
        return new IrTextBlock(
                x, y, 0,
                message.toString(),
                "Liberation Sans",
                11.0,
                0.0,
                "#4a4a4a");
    }

    private static int countByType(AfpPage page, AfpResource.ResourceType type) {
        return (int) page.resourceReferences().stream()
                .filter(r -> r.type() == type)
                .count();
    }

    private static boolean appendCounter(StringBuilder sb, boolean first, int count,
                                         String singular, String plural) {
        if (count <= 0) return first;
        if (!first) sb.append(", ");
        sb.append(count).append(' ').append(count == 1 ? singular : plural);
        return false;
    }

    private StubPageGenerator() {}
}
