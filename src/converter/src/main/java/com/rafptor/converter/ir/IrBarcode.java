package com.rafptor.converter.ir;

public final class IrBarcode extends IrElement {

    private final String type;
    private final String data;
    private final double width;
    private final double height;
    private final int moduleWidth;

    public IrBarcode(double x, double y, int zOrder,
                     String type, String data,
                     double width, double height, int moduleWidth) {
        super(x, y, zOrder);
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("barcode dimensions must be > 0");
        }
        this.type = type;
        this.data = data;
        this.width = width;
        this.height = height;
        this.moduleWidth = moduleWidth;
    }

    public String type() { return type; }
    public String data() { return data; }
    public double width() { return width; }
    public double height() { return height; }
    public int moduleWidth() { return moduleWidth; }
}
