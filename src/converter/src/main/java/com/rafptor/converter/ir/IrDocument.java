package com.rafptor.converter.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IrDocument {

    private final String name;
    private final List<IrPage> pages;
    private IrMetadata metadata;

    public IrDocument(String name) {
        this.name = name == null ? "" : name;
        this.pages = new ArrayList<>();
        this.metadata = new IrMetadata(this.name, 0, java.util.Map.of());
    }

    public String name() { return name; }
    public List<IrPage> pages() { return Collections.unmodifiableList(pages); }
    public IrMetadata metadata() { return metadata; }

    public void add(IrPage page) {
        if (page == null) {
            throw new IllegalArgumentException("page must not be null");
        }
        pages.add(page);
    }

    public void setMetadata(IrMetadata metadata) {
        this.metadata = metadata == null
                ? new IrMetadata(name, pages.size(), java.util.Map.of())
                : metadata;
    }
}
