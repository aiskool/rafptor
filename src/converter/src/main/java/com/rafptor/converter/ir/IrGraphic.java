package com.rafptor.converter.ir;

public final class IrGraphic extends IrElement {

    public enum Shape { LINE, RECT, RULE, ROUND_RECT }

    /** Dash patterns recognised by the renderer. Matches GOCA GSLT. */
    public enum StrokePattern { SOLID, DOTTED, SHORT_DASH, DASH_DOT, LONG_DASH }

    private final Shape shape;
    private final double x2;
    private final double y2;
    private final double width;
    private final double height;
    private final double cornerRadius;
    private final double lineWidth;
    private final String strokeColor;
    private final String fillColor;
    private final StrokePattern strokePattern;

    public IrGraphic(double x, double y, int zOrder, Shape shape,
                     double x2, double y2, double width, double height,
                     double lineWidth, String strokeColor, String fillColor) {
        this(x, y, zOrder, shape, x2, y2, width, height,
                0.0, lineWidth, strokeColor, fillColor, StrokePattern.SOLID);
    }

    public IrGraphic(double x, double y, int zOrder, Shape shape,
                     double x2, double y2, double width, double height,
                     double cornerRadius, double lineWidth,
                     String strokeColor, String fillColor,
                     StrokePattern strokePattern) {
        super(x, y, zOrder);
        if (shape == null) {
            throw new IllegalArgumentException("shape must not be null");
        }
        if (lineWidth < 0) {
            throw new IllegalArgumentException("lineWidth must be >= 0");
        }
        if (cornerRadius < 0) {
            throw new IllegalArgumentException("cornerRadius must be >= 0");
        }
        this.shape = shape;
        this.x2 = x2;
        this.y2 = y2;
        this.width = width;
        this.height = height;
        this.cornerRadius = cornerRadius;
        this.lineWidth = lineWidth;
        this.strokeColor = strokeColor == null ? "#000000" : strokeColor;
        this.fillColor = fillColor;
        this.strokePattern = strokePattern == null ? StrokePattern.SOLID : strokePattern;
    }

    public Shape shape() { return shape; }
    public double x2() { return x2; }
    public double y2() { return y2; }
    public double width() { return width; }
    public double height() { return height; }
    public double cornerRadius() { return cornerRadius; }
    public double lineWidth() { return lineWidth; }
    public String strokeColor() { return strokeColor; }
    public String fillColor() { return fillColor; }
    public StrokePattern strokePattern() { return strokePattern; }
}
