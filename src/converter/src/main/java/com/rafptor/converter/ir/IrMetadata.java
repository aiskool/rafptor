package com.rafptor.converter.ir;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IrMetadata {

    private final String documentName;
    private final int pageCount;
    private final Map<String, String> tags;

    public IrMetadata(String documentName, int pageCount, Map<String, String> tags) {
        this.documentName = documentName == null ? "" : documentName;
        this.pageCount = pageCount;
        this.tags = tags == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(tags));
    }

    public String documentName() { return documentName; }
    public int pageCount() { return pageCount; }
    public Map<String, String> tags() { return tags; }
}
