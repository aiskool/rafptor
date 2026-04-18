package com.rafptor.parser.model;

import com.rafptor.parser.ptoca.PtocaTextRun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One page of a parsed AFP document.
 */
public final class AfpPage {

    private final String name;
    private final List<PtocaTextRun> textRuns;
    private final List<AfpResource> resourceReferences;
    private final List<AfpStructuredField> structuredFields;
    private final List<AfpImageObject> images;
    private final Map<Integer, String> fontAssignments;
    private PageGeometry geometry;

    public AfpPage(String name) {
        this.name = name;
        this.textRuns = new ArrayList<>();
        this.resourceReferences = new ArrayList<>();
        this.structuredFields = new ArrayList<>();
        this.images = new ArrayList<>();
        this.fontAssignments = new HashMap<>();
    }

    public String name() {
        return name;
    }

    public PageGeometry geometry() {
        return geometry;
    }

    public void setGeometry(PageGeometry geometry) {
        this.geometry = geometry;
    }

    public List<PtocaTextRun> textRuns() {
        return Collections.unmodifiableList(textRuns);
    }

    public List<AfpResource> resourceReferences() {
        return Collections.unmodifiableList(resourceReferences);
    }

    public List<AfpStructuredField> structuredFields() {
        return Collections.unmodifiableList(structuredFields);
    }

    public void addTextRun(PtocaTextRun run) {
        if (run == null) {
            throw new IllegalArgumentException("run must not be null");
        }
        textRuns.add(run);
    }

    public void addResource(AfpResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
        resourceReferences.add(resource);
    }

    public void addStructuredField(AfpStructuredField sf) {
        if (sf == null) {
            throw new IllegalArgumentException("sf must not be null");
        }
        structuredFields.add(sf);
    }

    public List<AfpImageObject> images() {
        return Collections.unmodifiableList(images);
    }

    public void addImage(AfpImageObject image) {
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        images.add(image);
    }

    /** Local font id → coded-font resource name, as declared by MCF on this page. */
    public Map<Integer, String> fontAssignments() {
        return Collections.unmodifiableMap(fontAssignments);
    }

    public void putFontAssignment(int localId, String codedFontName) {
        fontAssignments.put(localId, codedFontName == null ? "" : codedFontName);
    }
}
