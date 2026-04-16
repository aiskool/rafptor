package com.rafptor.converter.ir;

/**
 * Base class for every positionable element of an {@link IrPage}.
 * Coordinates are expressed in PDF points (1/72 inch).
 */
public abstract sealed class IrElement permits IrTextBlock, IrImage, IrGraphic, IrBarcode, IrOverlay {

    protected final double x;
    protected final double y;
    protected final int zOrder;

    protected IrElement(double x, double y, int zOrder) {
        this.x = x;
        this.y = y;
        this.zOrder = zOrder;
    }

    public double x() { return x; }
    public double y() { return y; }
    public int zOrder() { return zOrder; }
}
