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
        for (int i = 0; i < runs.size(); i++) {
            PtocaTextRun run = runs.get(i);
            if (run.text().isEmpty()) {
                continue;
            }
            String resourceName = fontAssignments != null
                    ? fontAssignments.getOrDefault(run.localFontId(), "")
                    : "";
            FontMapping m = resolveMapping(resourceName, defaultMapping);
            double x = page.toPointsX(run.inlinePosition());
            double y = page.toPointsY(run.baselinePosition());
            double fontSize = m.defaultPointSize() * m.scaleFactor();
            String color = run.colorHex() == null || run.colorHex().isBlank()
                    ? "#000000" : run.colorHex();
            String text = run.text();
            // If the next run is on the same baseline and starts visibly to the
            // right of where this run's glyph box will end, append a space so
            // downstream text-layer consumers (search, accessibility, copy) see
            // a word boundary. Visual position remains anchored by the next
            // run's own AMI — the appended space character is absorbed into
            // this run's advance and does not shift later runs because each
            // IrTextBlock is emitted with its own absolute newLineAtOffset.
            if (shouldAppendWordBreak(runs, i, page, fontSize)) {
                text = text + ' ';
            }
            out.add(new IrTextBlock(
                    x, y, 0,
                    text,
                    m.trueTypeFont(),
                    fontSize,
                    0.0,
                    color));
        }
        return out;
    }

    /**
     * A PTOCA composer can emit consecutive TRN runs on the same baseline
     * without encoding any space character between them, relying on the
     * upstream font's glyph widths to leave a visible gap at AMI-declared
     * positions. When the glyph width of our substitute font happens to
     * match, the two runs render seamlessly and word boundaries disappear
     * from the extracted text layer. We synthesise that boundary here when
     * the next run's starting X is clearly to the right of this run's last
     * character: the PDF content then contains one ASCII space per logical
     * word break.
     */
    private static boolean shouldAppendWordBreak(List<PtocaTextRun> runs, int i,
                                                 IrPage page, double fontSize) {
        if (i + 1 >= runs.size()) {
            return false;
        }
        PtocaTextRun cur = runs.get(i);
        PtocaTextRun next = runs.get(i + 1);
        if (next.text().isEmpty()) {
            return false;
        }
        if (cur.baselinePosition() != next.baselinePosition()) {
            return false;
        }
        // If the current text already ends with whitespace, nothing to add.
        String t = cur.text();
        if (t.isEmpty() || Character.isWhitespace(t.charAt(t.length() - 1))) {
            return false;
        }
        // If the next run begins with whitespace, skip too.
        if (Character.isWhitespace(next.text().charAt(0))) {
            return false;
        }
        double curX = page.toPointsX(cur.inlinePosition());
        double nextX = page.toPointsX(next.inlinePosition());
        // Estimate current run's rendered width: ~0.55 × fontSize per average
        // character for a sans-serif face. This is intentionally conservative
        // — when it under-estimates we emit a redundant space, which is
        // harmless; when it over-estimates we drop the space and the runs
        // remain glued, which is the status quo.
        double estWidth = t.length() * fontSize * 0.55;
        double gapPt = nextX - (curX + estWidth);
        return gapPt >= fontSize * 0.15; // ~1.5pt at 10pt — smaller than a word space
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
        if (resourceName.startsWith("C0")) {
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
