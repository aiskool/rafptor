package com.rafptor.converter.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IrPage {

    private final String name;
    private final double widthPt;
    private final double heightPt;
    private final int xResolution;
    private final int yResolution;
    private final List<IrElement> elements;

    /**
     * Legacy constructor — single resolution used for both axes. Kept for
     * existing fixtures and tests; prefer the two-axis constructor below.
     */
    public IrPage(String name, double widthPt, double heightPt, int afpResolution) {
        this(name, widthPt, heightPt, afpResolution, afpResolution);
    }

    /**
     * Two-axis constructor — used when the Presentation Text Descriptor
     * declares distinct X/Y L-units-per-inch (common at 1440 × 1440 on
     * banking statements produced by IBM composers).
     */
    public IrPage(String name, double widthPt, double heightPt,
                  int xResolution, int yResolution) {
        this.name = name == null ? "" : name;
        if (widthPt <= 0 || heightPt <= 0) {
            throw new IllegalArgumentException("page dimensions must be > 0");
        }
        if (xResolution <= 0 || yResolution <= 0) {
            throw new IllegalArgumentException("resolutions must be > 0");
        }
        this.widthPt = widthPt;
        this.heightPt = heightPt;
        this.xResolution = xResolution;
        this.yResolution = yResolution;
        this.elements = new ArrayList<>();
    }

    public String name() { return name; }
    public double widthPt() { return widthPt; }
    public double heightPt() { return heightPt; }
    public int afpResolution() { return xResolution; }
    public int xResolution() { return xResolution; }
    public int yResolution() { return yResolution; }
    public List<IrElement> elements() { return Collections.unmodifiableList(elements); }

    public void add(IrElement e) {
        if (e == null) {
            throw new IllegalArgumentException("element must not be null");
        }
        elements.add(e);
    }

    /** Converts AFP L-units to PDF points using the X-axis resolution. */
    public double toPoints(int lunits) {
        return toPointsX(lunits);
    }

    /** Converts inline (AMI — X-axis) L-units to PDF points. */
    public double toPointsX(int lunits) {
        return (double) lunits * 72.0 / (double) xResolution;
    }

    /** Converts baseline (AMB — Y-axis) L-units to PDF points. */
    public double toPointsY(int lunits) {
        return (double) lunits * 72.0 / (double) yResolution;
    }
}
