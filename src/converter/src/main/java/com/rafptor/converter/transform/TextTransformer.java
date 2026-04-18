package com.rafptor.converter.transform;

import com.rafptor.converter.font.FontMapper;
import com.rafptor.converter.font.FontMapping;
import com.rafptor.converter.ir.IrPage;
import com.rafptor.converter.ir.IrTextBlock;
import com.rafptor.parser.ptoca.PtocaTextRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts parser {@link PtocaTextRun}s (already decoded) to positioned
 * {@link IrTextBlock}s on an {@link IrPage}.
 *
 * <p>Coordinate conversion: PTOCA positions are in AFP L-units at the page
 * resolution. The IR expresses positions in PDF points (72 dpi).
 * {@link IrPage#toPoints(int)} performs the conversion.
 *
 * <p>Font resolution: the PTOCA {@code SCFL} control sequence carries a
 * local font id. The page's MCF records map that id to a coded-font resource
 * name (e.g. {@code C0H200F0}, {@code C0N20080}, {@code C0S20110}). The two
 * leading letters of the resource name convey the family (Courier, Sonoran
 * Sans, Sonoran Serif, Letter Gothic, Times Roman …), from which we derive
 * the {@link FontMapping} candidate.
 */
public final class TextTransformer {

    private final FontMapper fontMapper;

    public TextTransformer(FontMapper fontMapper) {
        if (fontMapper == null) {
            throw new IllegalArgumentException("fontMapper must not be null");
        }
        this.fontMapper = fontMapper;
    }

    /**
     * Legacy entry point without font assignments — keeps the default mapping.
     * Used by existing unit tests.
     */
    public List<IrTextBlock> transform(List<PtocaTextRun> runs, IrPage page) {
        return transform(runs, page, Map.of());
    }

    public List<IrTextBlock> transform(List<PtocaTextRun> runs, IrPage page,
                                       Map<Integer, String> fontAssignments) {
        if (runs == null || page == null) {
            return List.of();
        }
        List<IrTextBlock> out = new ArrayList<>(runs.size());
        FontMapping defaultMapping = fontMapper.defaultMapping();
        for (PtocaTextRun run : runs) {
            if (run.text().isEmpty()) {
                continue;
            }
            String resourceName = fontAssignments != null
                    ? fontAssignments.getOrDefault(run.localFontId(), "")
                    : "";
            FontMapping m = resolveMapping(resourceName, defaultMapping);
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

    /**
     * Pick the most specific mapping given a coded-font resource name. Rules
     * follow IBM's own two-letter family convention:
     * <ul>
     *   <li>{@code C0H*}, {@code C0420*}, {@code X0*C*} → Courier / monospace</li>
     *   <li>{@code C0N*}, {@code C0GT*} → Sonoran Sans</li>
     *   <li>{@code C0S*}, {@code C0TM*} → Sonoran Serif / Times Roman</li>
     *   <li>{@code C0LG*} → Letter Gothic</li>
     *   <li>{@code C0PE*} → Prestige Elite</li>
     * </ul>
     * Unrecognised prefixes fall back to the default mapping.
     */
    private FontMapping resolveMapping(String resourceName, FontMapping fallback) {
        if (resourceName == null || resourceName.length() < 4) {
            return fallback;
        }
        String prefix = resourceName.substring(0, 4).toUpperCase();
        FontMapping m = fontMapper.findByCharsetPrefix(prefix);
        if (m != null) return m;

        // Heuristic on family letter (position 2 of C0XX* names).
        if (resourceName.length() >= 3 && resourceName.startsWith("C0")) {
            char fam = resourceName.charAt(2);
            FontMapping byFamily = switch (fam) {
                case 'H' -> fontMapper.findByCharsetPrefix("C0H2");
                case 'N' -> fontMapper.findByCharsetPrefix("C0N2");
                case 'S' -> fontMapper.findByCharsetPrefix("C0S2");
                case 'L' -> fontMapper.findByCharsetPrefix("C0LG");
                case 'P' -> fontMapper.findByCharsetPrefix("C0PE");
                case 'T' -> fontMapper.findByCharsetPrefix("C0TM");
                case 'G' -> fontMapper.findByCharsetPrefix("C0GT");
                default -> null;
            };
            if (byFamily != null) return byFamily;
        }
        return fallback;
    }
}
