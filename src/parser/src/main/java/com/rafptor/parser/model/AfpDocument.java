package com.rafptor.parser.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Top-level AST node produced by {@code RafptorParser#parse}.
 */
public final class AfpDocument {

    private final String name;
    private final List<AfpPage> pages;
    private final List<AfpResource> resourceReferences;
    private final Map<String, String> tags;
    private final List<AfpStructuredField> structuredFields;

    public AfpDocument(String name) {
        this.name = name;
        this.pages = new ArrayList<>();
        this.resourceReferences = new ArrayList<>();
        this.tags = new HashMap<>();
        this.structuredFields = new ArrayList<>();
    }

    public String name() {
        return name;
    }

    public List<AfpPage> pages() {
        return Collections.unmodifiableList(pages);
    }

    public List<AfpResource> resourceReferences() {
        return Collections.unmodifiableList(resourceReferences);
    }

    public Map<String, String> tags() {
        return Collections.unmodifiableMap(tags);
    }

    public List<AfpStructuredField> structuredFields() {
        return Collections.unmodifiableList(structuredFields);
    }

    public void addPage(AfpPage page) {
        if (page == null) {
            throw new IllegalArgumentException("page must not be null");
        }
        pages.add(page);
    }

    public void addResource(AfpResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
        resourceReferences.add(resource);
    }

    public void putTag(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("tag key must not be null");
        }
        tags.put(key, value);
    }

    public void addStructuredField(AfpStructuredField sf) {
        if (sf == null) {
            throw new IllegalArgumentException("sf must not be null");
        }
        structuredFields.add(sf);
    }
}
