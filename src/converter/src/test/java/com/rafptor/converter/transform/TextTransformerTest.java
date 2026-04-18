package com.rafptor.converter.transform;

import com.rafptor.converter.font.StandardFontMapper;
import com.rafptor.converter.ir.IrPage;
import com.rafptor.converter.ir.IrTextBlock;
import com.rafptor.parser.ptoca.PtocaTextRun;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextTransformerTest {

    @Test
    void convertsPtocaRunToPoints() {
        IrPage page = new IrPage("p", 595, 842, 240);
        PtocaTextRun run = new PtocaTextRun(1, 480, 240, "HELLO");
        List<IrTextBlock> blocks = new TextTransformer(new StandardFontMapper()).transform(List.of(run), page);
        assertEquals(1, blocks.size());
        IrTextBlock b = blocks.get(0);
        assertEquals("HELLO", b.text());
        // 240 L-units @ 240 dpi = 72 points
        assertEquals(72.0, b.x(), 1e-6);
        assertEquals(144.0, b.y(), 1e-6);
    }

    @Test
    void skipsEmptyRuns() {
        IrPage page = new IrPage("p", 595, 842, 240);
        List<IrTextBlock> blocks = new TextTransformer(new StandardFontMapper())
                .transform(List.of(new PtocaTextRun(0, 0, 0, "")), page);
        assertTrue(blocks.isEmpty());
    }

    @Test
    void routesMonoVsSansVsSerifByCharsetPrefix() {
        IrPage page = new IrPage("p", 595, 842, 240);
        TextTransformer t = new TextTransformer(new StandardFontMapper());
        var monoRun = new PtocaTextRun(1, 0, 0, "Mono");
        var sansRun = new PtocaTextRun(2, 0, 0, "Sans");
        var serifRun = new PtocaTextRun(3, 0, 0, "Serif");
        var fontAssignments = java.util.Map.of(
                1, "C0H20000",   // IBM Courier  -> Liberation Mono
                2, "C0N20080",   // IBM Sonoran Sans Serif -> Liberation Sans
                3, "C0S20110");  // IBM Sonoran Serif -> Liberation Serif
        List<IrTextBlock> blocks = t.transform(
                List.of(monoRun, sansRun, serifRun), page, fontAssignments);
        assertEquals(3, blocks.size());
        assertTrue(blocks.get(0).fontName().toLowerCase().contains("mono"),
                "mono run: " + blocks.get(0).fontName());
        assertTrue(blocks.get(1).fontName().toLowerCase().contains("sans"),
                "sans run: " + blocks.get(1).fontName());
        assertTrue(blocks.get(2).fontName().toLowerCase().contains("serif"),
                "serif run: " + blocks.get(2).fontName());
    }
}
