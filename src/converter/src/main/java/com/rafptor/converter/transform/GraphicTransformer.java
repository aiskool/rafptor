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
            double originX = page.toPointsX(g.xOriginLUnits());
            double originY = page.toPointsY(g.yOriginLUnits());
            for (GocaDecoder.DrawOrder o : orders) {
                if (o instanceof GocaDecoder.SetLineWidth slw) {
                    // GOCA line width is in design units; scale the same way
                    // as coordinates so the stroke reads naturally at any
                    // resolution. 1 design unit at 240 dpi ≈ 0.3 pt.
                    lineWidth = Math.max(0.1, page.toPointsX(
                            (int) Math.round(slw.widthDesignUnits())));
                } else if (o instanceof GocaDecoder.SetColor sc) {
                    stroke = sc.hexRgb();
                } else if (o instanceof GocaDecoder.Line l) {
                    double x1 = originX + page.toPointsX(l.x1());
                    double y1 = originY + page.toPointsY(l.y1());
                    double x2 = originX + page.toPointsX(l.x2());
                    double y2 = originY + page.toPointsY(l.y2());
                    out.add(new IrGraphic(
                            x1, y1, 0, IrGraphic.Shape.LINE,
                            x2, y2, 0, 0, lineWidth, stroke, null));
                } else if (o instanceof GocaDecoder.Rect r) {
                    double x = originX + page.toPointsX(r.x());
                    double y = originY + page.toPointsY(r.y());
                    double w = page.toPointsX(r.width());
                    double h = page.toPointsY(r.height());
                    out.add(new IrGraphic(
                            x, y, 0, IrGraphic.Shape.RECT,
                            0, 0, w, h, lineWidth, stroke, null));
                }
            }
        }
        return out;
    }

    public String notImplementedWarning() {
        return "GOCA graphics transformation is not implemented yet; graphics dropped.";
    }
}
