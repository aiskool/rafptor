package com.rafptor.converter.ir;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IrDocumentTest {

    @Test
    void addPagePreservesOrder() {
        IrDocument doc = new IrDocument("TEST");
        doc.add(new IrPage("p1", 595, 842, 240));
        doc.add(new IrPage("p2", 595, 842, 240));
        assertEquals(2, doc.pages().size());
        assertEquals("p1", doc.pages().get(0).name());
        assertEquals("p2", doc.pages().get(1).name());
    }

    @Test
    void pagesListIsUnmodifiable() {
        IrDocument doc = new IrDocument("TEST");
        assertThrows(UnsupportedOperationException.class,
                () -> doc.pages().add(new IrPage("x", 595, 842, 240)));
    }

    @Test
    void toPointsConvertsLunits() {
        IrPage page = new IrPage("p", 595, 842, 240);
        assertEquals(30.0, page.toPoints(100), 0.0001);
    }

    @Test
    void setMetadataUpdatesValue() {
        IrDocument doc = new IrDocument("TEST");
        doc.setMetadata(new IrMetadata("TEST", 1, Map.of("ClientId", "ACME001")));
        assertEquals("ACME001", doc.metadata().tags().get("ClientId"));
    }

    @Test
    void textBlockRejectsBlankFont() {
        assertThrows(IllegalArgumentException.class,
                () -> new IrTextBlock(0, 0, 0, "x", " ", 10, 0, null));
    }

    @Test
    void pageRejectsNegativeDimensions() {
        assertThrows(IllegalArgumentException.class,
                () -> new IrPage("p", -1, 842, 240));
    }

    @Test
    void elementsSortableByZOrder() {
        IrPage page = new IrPage("p", 595, 842, 240);
        page.add(new IrTextBlock(0, 0, 5, "z5", "Helvetica", 10, 0, null));
        page.add(new IrTextBlock(0, 0, -1, "z-1", "Helvetica", 10, 0, null));
        int first = page.elements().get(0).zOrder();
        int second = page.elements().get(1).zOrder();
        assertNotNull(page.elements());
        assertEquals(5, first);
        assertEquals(-1, second);
    }
}
