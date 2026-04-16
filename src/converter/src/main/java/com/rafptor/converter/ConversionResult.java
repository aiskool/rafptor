package com.rafptor.converter;

import com.rafptor.converter.validation.ValidationReport;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConversionResult {

    public enum Status {
        SUCCESS, WARNING, FAILED
    }

    private final String sourceDocument;
    private final Path outputPath;
    private final long outputSize;
    private final int pageCount;
    private final Status status;
    private final String error;
    private final List<String> warnings;
    private final ValidationReport validation;
    private final Instant startTime;
    private final Instant endTime;
    private final long durationMs;

    private ConversionResult(Builder b) {
        this.sourceDocument = b.sourceDocument;
        this.outputPath = b.outputPath;
        this.outputSize = b.outputSize;
        this.pageCount = b.pageCount;
        this.status = b.status;
        this.error = b.error;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(b.warnings));
        this.validation = b.validation;
        this.startTime = b.startTime;
        this.endTime = b.endTime;
        this.durationMs = b.durationMs;
    }

    public String sourceDocument() { return sourceDocument; }
    public Path outputPath() { return outputPath; }
    public long outputSize() { return outputSize; }
    public int pageCount() { return pageCount; }
    public Status status() { return status; }
    public String error() { return error; }
    public List<String> warnings() { return warnings; }
    public ValidationReport validation() { return validation; }
    public Instant startTime() { return startTime; }
    public Instant endTime() { return endTime; }
    public long durationMs() { return durationMs; }

    /**
     * Returns a JSON representation. Hand-written (no Jackson dependency) to keep
     * the converter footprint minimal. All strings are JSON-escaped.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        appendString(sb, "source_document", sourceDocument); sb.append(',');
        appendString(sb, "output_path", outputPath == null ? null : outputPath.toString()); sb.append(',');
        sb.append("\"output_size\":").append(outputSize).append(',');
        sb.append("\"page_count\":").append(pageCount).append(',');
        appendString(sb, "status", status.name()); sb.append(',');
        appendString(sb, "error", error); sb.append(',');
        sb.append("\"warnings\":").append(jsonStringArray(warnings)).append(',');
        sb.append("\"validation_passed\":").append(validation != null && validation.isPassed()).append(',');
        appendString(sb, "start_time", startTime == null ? null : startTime.toString()); sb.append(',');
        appendString(sb, "end_time", endTime == null ? null : endTime.toString()); sb.append(',');
        sb.append("\"duration_ms\":").append(durationMs);
        sb.append('}');
        return sb.toString();
    }

    private static void appendString(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append("\":");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"').append(escape(value)).append('"');
        }
    }

    private static String jsonStringArray(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(escape(items.get(i))).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String sourceDocument;
        private Path outputPath;
        private long outputSize;
        private int pageCount;
        private Status status = Status.FAILED;
        private String error;
        private final List<String> warnings = new ArrayList<>();
        private ValidationReport validation;
        private Instant startTime;
        private Instant endTime;
        private long durationMs;

        public Builder sourceDocument(String v) { this.sourceDocument = v; return this; }
        public Builder outputPath(Path v) { this.outputPath = v; return this; }
        public Builder outputSize(long v) { this.outputSize = v; return this; }
        public Builder pageCount(int v) { this.pageCount = v; return this; }
        public Builder status(Status v) { this.status = v; return this; }
        public Builder error(String v) { this.error = v; return this; }
        public Builder addWarning(String v) { if (v != null) warnings.add(v); return this; }
        public Builder addWarnings(List<String> v) { if (v != null) warnings.addAll(v); return this; }
        public Builder validation(ValidationReport v) { this.validation = v; return this; }
        public Builder startTime(Instant v) { this.startTime = v; return this; }
        public Builder endTime(Instant v) { this.endTime = v; return this; }
        public Builder durationMs(long v) { this.durationMs = v; return this; }
        public ConversionResult build() { return new ConversionResult(this); }
    }
}
