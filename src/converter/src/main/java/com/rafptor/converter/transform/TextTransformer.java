package com.rafptor.converter.transform;

import com.rafptor.converter.font.FontMapper;
import com.rafptor.converter.font.FontMapping;
import com.rafptor.converter.ir.IrPage;
import com.rafptor.converter.ir.IrTextBlock;
import com.rafptor.parser.ptoca.PtocaTextRun;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts parser {@link PtocaTextRun}s (already decoded) to positioned
 * {@link IrTextBlock}s on an {@link IrPage}.
 *
 * <p>Coordinate conversion: PTOCA positions are in AFP L-units at the page
 * resolution (240 dpi by default). The IR expresses positions in PDF points
 * (72 dpi). {@link IrPage#toPoints(int)} performs the conversion.
 */
public final class TextTransformer {

    private final FontMapper fontMapper;

    public TextTransformer(FontMapper fontMapper) {
        if (fontMapper == null) {
            throw new IllegalArgumentException("fontMapper must not be null");
        }
        this.fontMapper = fontMapper;
    }

    public List<IrTextBlock> transform(List<PtocaTextRun> runs, IrPage page) {
        if (runs == null || page == null) {
            return List.of();
        }
        List<IrTextBlock> out = new ArrayList<>(runs.size());
        FontMapping defaultMapping = fontMapper.defaultMapping();
        for (PtocaTextRun run : runs) {
            if (run.text().isEmpty()) {
                continue;
            }
            // Parser stores the PTOCA local font id; we do not yet resolve MCF
            // to a concrete code-page — all runs currently use the default
            // mapping. The mapping chain is in place for the next iteration.
            FontMapping m = defaultMapping;
            double x = page.toPoints(run.inlinePosition());
            double y = page.toPoints(run.baselinePosition());
            out.add(new IrTextBlock(
                    x, y, 0,
                    run.text(),
                    m.trueTypeFont(),
                    m.defaultPointSize() * m.scaleFactor(),
                    0.0,
                    "#000000"));
        }
        return out;
    }
}
