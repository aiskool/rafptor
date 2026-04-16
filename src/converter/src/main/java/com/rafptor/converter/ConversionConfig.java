package com.rafptor.converter;

import java.nio.file.Path;

/**
 * Immutable configuration for a single conversion run.
 */
public final class ConversionConfig {

    public static final long DEFAULT_MAX_OUTPUT_BYTES = 1024L * 1024L * 1024L;
    public static final int DEFAULT_AFP_RESOLUTION = 240;
    public static final double DEFAULT_PAGE_WIDTH_PT = 595.0;
    public static final double DEFAULT_PAGE_HEIGHT_PT = 842.0;

    private final boolean pdfA;
    private final String documentTitle;
    private final String creator;
    private final long maxOutputBytes;
    private final Path fontResourcesDir;
    private final Path iccProfilePath;
    private final int afpResolution;
    private final double defaultPageWidthPt;
    private final double defaultPageHeightPt;

    private ConversionConfig(Builder b) {
        this.pdfA = b.pdfA;
        this.documentTitle = b.documentTitle;
        this.creator = b.creator;
        this.maxOutputBytes = b.maxOutputBytes;
        this.fontResourcesDir = b.fontResourcesDir;
        this.iccProfilePath = b.iccProfilePath;
        this.afpResolution = b.afpResolution;
        this.defaultPageWidthPt = b.defaultPageWidthPt;
        this.defaultPageHeightPt = b.defaultPageHeightPt;
    }

    public static ConversionConfig defaults() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isPdfA() { return pdfA; }
    public String documentTitle() { return documentTitle; }
    public String creator() { return creator; }
    public long maxOutputBytes() { return maxOutputBytes; }
    public Path fontResourcesDir() { return fontResourcesDir; }
    public Path iccProfilePath() { return iccProfilePath; }
    public int afpResolution() { return afpResolution; }
    public double defaultPageWidthPt() { return defaultPageWidthPt; }
    public double defaultPageHeightPt() { return defaultPageHeightPt; }

    public static final class Builder {
        private boolean pdfA = false;
        private String documentTitle = "Rafptor converted document";
        private String creator = "Rafptor Converter";
        private long maxOutputBytes = DEFAULT_MAX_OUTPUT_BYTES;
        private Path fontResourcesDir;
        private Path iccProfilePath;
        private int afpResolution = DEFAULT_AFP_RESOLUTION;
        private double defaultPageWidthPt = DEFAULT_PAGE_WIDTH_PT;
        private double defaultPageHeightPt = DEFAULT_PAGE_HEIGHT_PT;

        public Builder pdfA(boolean v) { this.pdfA = v; return this; }
        public Builder documentTitle(String v) { this.documentTitle = v; return this; }
        public Builder creator(String v) { this.creator = v; return this; }
        public Builder maxOutputBytes(long v) { this.maxOutputBytes = v; return this; }
        public Builder fontResourcesDir(Path v) { this.fontResourcesDir = v; return this; }
        public Builder iccProfilePath(Path v) { this.iccProfilePath = v; return this; }
        public Builder afpResolution(int v) { this.afpResolution = v; return this; }
        public Builder defaultPageWidthPt(double v) { this.defaultPageWidthPt = v; return this; }
        public Builder defaultPageHeightPt(double v) { this.defaultPageHeightPt = v; return this; }

        public ConversionConfig build() {
            if (afpResolution <= 0) {
                throw new IllegalArgumentException("afpResolution must be > 0");
            }
            if (maxOutputBytes <= 0) {
                throw new IllegalArgumentException("maxOutputBytes must be > 0");
            }
            if (defaultPageWidthPt <= 0 || defaultPageHeightPt <= 0) {
                throw new IllegalArgumentException("page dimensions must be > 0");
            }
            return new ConversionConfig(this);
        }
    }
}
