package com.rafptor.converter.ir;

public final class IrTextBlock extends IrElement {

    private final String text;
    private final String fontName;
    private final double fontSize;
    private final double charSpacing;
    private final String color;

    public IrTextBlock(double x, double y, int zOrder,
                       String text, String fontName, double fontSize,
                       double charSpacing, String color) {
        super(x, y, zOrder);
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (fontName == null || fontName.isBlank()) {
            throw new IllegalArgumentException("fontName must not be blank");
        }
        if (fontSize <= 0) {
            throw new IllegalArgumentException("fontSize must be > 0");
        }
        this.text = text;
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.charSpacing = charSpacing;
        this.color = color == null ? "#000000" : color;
    }

    public String text() { return text; }
    public String fontName() { return fontName; }
    public double fontSize() { return fontSize; }
    public double charSpacing() { return charSpacing; }
    public String color() { return color; }
}
