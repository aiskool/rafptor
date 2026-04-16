package com.rafptor.converter.ir;

public final class IrGraphic extends IrElement {

    public enum Shape { LINE, RECT, RULE }

    private final Shape shape;
    private final double x2;
    private final double y2;
    private final double width;
    private final double height;
    private final double lineWidth;
    private final String strokeColor;
    private final String fillColor;

    public IrGraphic(double x, double y, int zOrder, Shape shape,
                     double x2, double y2, double width, double height,
                     double lineWidth, String strokeColor, String fillColor) {
        super(x, y, zOrder);
        if (shape == null) {
            throw new IllegalArgumentException("shape must not be null");
        }
        if (lineWidth < 0) {
            throw new IllegalArgumentException("lineWidth must be >= 0");
        }
        this.shape = shape;
        this.x2 = x2;
        this.y2 = y2;
        this.width = width;
        this.height = height;
        this.lineWidth = lineWidth;
        this.strokeColor = strokeColor == null ? "#000000" : strokeColor;
        this.fillColor = fillColor;
    }

    public Shape shape() { return shape; }
    public double x2() { return x2; }
    public double y2() { return y2; }
    public double width() { return width; }
    public double height() { return height; }
    public double lineWidth() { return lineWidth; }
    public String strokeColor() { return strokeColor; }
    public String fillColor() { return fillColor; }
}
