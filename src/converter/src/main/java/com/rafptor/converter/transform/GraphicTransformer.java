package com.rafptor.converter.transform;

import com.rafptor.converter.ir.IrGraphic;
import com.rafptor.converter.ir.IrPage;
import com.rafptor.converter.render.GocaDecoder;
import com.rafptor.parser.model.AfpGraphicObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Walks {@link AfpGraphicObject}s collected by the parser, runs their
 * payloads through {@link GocaDecoder} and produces {@link IrGraphic}
 * records in PDF points ready for the {@link com.rafptor.converter.render.PdfRenderer}.
 *
 * <p>GOCA coordinates are in the page's X/Y L-units; we convert to PDF
 * points via {@link IrPage#toPointsX(int)} / {@link IrPage#toPointsY(int)}.
 * Line width is interpreted as a linear fraction of the page resolution
 * (0.25 pt per design unit at the standard 240 dpi) so hairlines remain
 * visible without overpowering the layout.
 */
public final class GraphicTransformer {

    private static final double DEFAULT_LINE_WIDTH_PT = 0.5;

    public List<IrGraphic> transform(List<AfpGraphicObject> graphics, IrPage page) {
        if (graphics == null || graphics.isEmpty() || page == null) {
            return List.of();
        }
        List<IrGraphic> out = new ArrayList<>();
        for (AfpGraphicObject g : graphics) {
            List<GocaDecoder.DrawOrder> orders = GocaDecoder.decode(g);
            if (orders.isEmpty()) continue;
            double lineWidth = DEFAULT_LINE_WIDTH_PT;
            String stroke = "#000000";
            IrGraphic.StrokePattern pattern = IrGraphic.StrokePattern.SOLID;
            double originX = page.toPointsX(g.xOriginLUnits());
            double originY = page.toPointsY(g.yOriginLUnits());
            for (GocaDecoder.DrawOrder o : orders) {
                if (o instanceof GocaDecoder.SetLineWidth slw) {
                    lineWidth = Math.max(0.1, page.toPointsX(
                            (int) Math.round(slw.widthDesignUnits())));
                } else if (o instanceof GocaDecoder.SetColor sc) {
                    stroke = sc.hexRgb();
                } else if (o instanceof GocaDecoder.SetLineType slt) {
                    pattern = mapPattern(slt.pattern());
                } else if (o instanceof GocaDecoder.Line l) {
                    double x1 = originX + page.toPointsX(l.x1());
                    double y1 = originY + page.toPointsY(l.y1());
                    double x2 = originX + page.toPointsX(l.x2());
                    double y2 = originY + page.toPointsY(l.y2());
                    out.add(new IrGraphic(
                            x1, y1, 0, IrGraphic.Shape.LINE,
                            x2, y2, 0, 0, 0.0, lineWidth, stroke, null, pattern));
                } else if (o instanceof GocaDecoder.Rect r) {
                    double x = originX + page.toPointsX(r.x());
                    double y = originY + page.toPointsY(r.y());
                    double w = page.toPointsX(r.width());
                    double h = page.toPointsY(r.height());
                    out.add(new IrGraphic(
                            x, y, 0, IrGraphic.Shape.RECT,
                            0, 0, w, h, 0.0, lineWidth, stroke, null, pattern));
                } else if (o instanceof GocaDecoder.RoundedRect rr) {
                    double x = originX + page.toPointsX(rr.x());
                    double y = originY + page.toPointsY(rr.y());
                    double w = page.toPointsX(rr.width());
                    double h = page.toPointsY(rr.height());
                    double r = Math.max(
                            page.toPointsX(rr.rx()),
                            page.toPointsY(rr.ry()));
                    out.add(new IrGraphic(
                            x, y, 0, IrGraphic.Shape.ROUND_RECT,
                            0, 0, w, h, r, lineWidth, stroke, null, pattern));
                }
            }
        }
        return out;
    }

    private static IrGraphic.StrokePattern mapPattern(GocaDecoder.LineType lt) {
        return switch (lt) {
            case DOTTED     -> IrGraphic.StrokePattern.DOTTED;
            case SHORT_DASH -> IrGraphic.StrokePattern.SHORT_DASH;
            case DASH_DOT   -> IrGraphic.StrokePattern.DASH_DOT;
            case LONG_DASH  -> IrGraphic.StrokePattern.LONG_DASH;
            default         -> IrGraphic.StrokePattern.SOLID;
        };
    }

    public String notImplementedWarning() {
        return "GOCA graphics transformation is not implemented yet; graphics dropped.";
    }
}
