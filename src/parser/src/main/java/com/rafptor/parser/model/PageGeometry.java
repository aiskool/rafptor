package com.rafptor.parser.model;

/**
 * Page geometry derived from the MO:DCA Page Descriptor (PGD) and
 * Presentation Text Descriptor (PTD) structured fields.
 *
 * <p>Coordinates on a page are expressed in <b>L-units</b>, a unit whose physical
 * size is governed by the resolution in L-units per inch (typically 240, 300, 480
 * or 1440 dpi). The PGD carries the page width and height in L-units plus the
 * two-axis resolution. The PTD carries the presentation text resolution which
 * may differ from the page resolution — it governs how PTOCA move-inline and
 * move-baseline values are interpreted.
 *
 * <p>All units are integers on the wire; expose them unchanged so the
 * converter can do the 72-dpi conversion at the right point.
 */
public record PageGeometry(
        int widthLUnits,
        int heightLUnits,
        int xResolution,
        int yResolution,
        int ptxXResolution,
        int ptxYResolution) {

    public PageGeometry {
        if (widthLUnits < 0 || heightLUnits < 0) {
            throw new IllegalArgumentException("dimensions must not be negative");
        }
        if (xResolution <= 0 || yResolution <= 0) {
            throw new IllegalArgumentException("resolutions must be > 0");
        }
        if (ptxXResolution < 0 || ptxYResolution < 0) {
            throw new IllegalArgumentException("ptx resolutions must not be negative");
        }
    }

    /** Width converted to PDF points (72 dpi). */
    public double widthPt() {
        return (double) widthLUnits * 72.0 / (double) xResolution;
    }

    /** Height converted to PDF points (72 dpi). */
    public double heightPt() {
        return (double) heightLUnits * 72.0 / (double) yResolution;
    }

    /** Effective X resolution to convert PTOCA inline coordinates to points. */
    public int effectivePtxResolution() {
        return effectivePtxXResolution();
    }

    /** Effective X resolution (PTD if declared, otherwise PGD). */
    public int effectivePtxXResolution() {
        return ptxXResolution > 0 ? ptxXResolution : xResolution;
    }

    /** Effective Y resolution (PTD if declared, otherwise PGD). */
    public int effectivePtxYResolution() {
        return ptxYResolution > 0 ? ptxYResolution : yResolution;
    }
}
