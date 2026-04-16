package com.rafptor.converter.ir;

public final class IrImage extends IrElement {

    private final byte[] data;
    private final String format;
    private final double width;
    private final double height;
    private final int originalDpi;

    public IrImage(double x, double y, int zOrder,
                   byte[] data, String format,
                   double width, double height, int originalDpi) {
        super(x, y, zOrder);
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("format must not be blank");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("image dimensions must be > 0");
        }
        this.data = data.clone();
        this.format = format;
        this.width = width;
        this.height = height;
        this.originalDpi = originalDpi;
    }

    public byte[] data() { return data.clone(); }
    public String format() { return format; }
    public double width() { return width; }
    public double height() { return height; }
    public int originalDpi() { return originalDpi; }
}
