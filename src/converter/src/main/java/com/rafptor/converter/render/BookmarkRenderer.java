package com.rafptor.converter.render;

import com.rafptor.converter.ir.IrDocument;
import com.rafptor.converter.ir.IrPage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

/**
 * Phase 6 — writes an outline (bookmarks) tree on a PDF when the IR
 * carries named pages (IRs sourced from AFP Named Page Groups / IDX
 * records). The outline lets Acrobat users jump to a named statement,
 * invoice or customer section directly.
 *
 * <p>The heuristic today: every page that has a non-empty name, and whose
 * name differs from its immediate predecessor, gets a top-level outline
 * item. A follow-up may nest groups of pages with a common prefix.
 */
public final class BookmarkRenderer {

    private BookmarkRenderer() {
    }

    public static void apply(PDDocument pdf, IrDocument ir) {
        if (pdf == null || ir == null || ir.pages().isEmpty()) return;
        PDDocumentOutline outline = new PDDocumentOutline();
        pdf.getDocumentCatalog().setDocumentOutline(outline);
        String prevName = null;
        int idx = 0;
        for (IrPage page : ir.pages()) {
            String name = page.name();
            if (name != null && !name.isBlank() && !name.equals(prevName)
                    && idx < pdf.getNumberOfPages()) {
                PDOutlineItem item = new PDOutlineItem();
                item.setTitle(name);
                PDPage target = pdf.getPage(idx);
                item.setDestination(target);
                outline.addLast(item);
                prevName = name;
            }
            idx++;
        }
        outline.openNode();
    }
}
