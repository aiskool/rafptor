package com.rafptor.converter.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IrPage {

    private final String name;
    private final double widthPt;
    private final double heightPt;
    private final int afpResolution;
    private final List<IrElement> elements;

    public IrPage(String name, double widthPt, double heightPt, int afpResolution) {
        this.name = name == null ? "" : name;
        if (widthPt <= 0 || heightPt <= 0) {
            throw new IllegalArgumentException("page dimensions must be > 0");
        }
        if (afpResolution <= 0) {
            throw new IllegalArgumentException("afpResolution must be > 0");
        }
        this.widthPt = widthPt;
        this.heightPt = heightPt;
        this.afpResolution = afpResolution;
        this.elements = new ArrayList<>();
    }

    public String name() { return name; }
    public double widthPt() { return widthPt; }
    public double heightPt() { return heightPt; }
    public int afpResolution() { return afpResolution; }
    public List<IrElement> elements() { return Collections.unmodifiableList(elements); }

    public void add(IrElement e) {
        if (e == null) {
            throw new IllegalArgumentException("element must not be null");
        }
        elements.add(e);
    }

    /** Converts AFP L-units to PDF points using the page resolution. */
    public double toPoints(int lunits) {
        return (double) lunits * 72.0 / (double) afpResolution;
    }
}
