package com.rafptor.parser.model;

import com.rafptor.parser.audit.ByteAccountingReport;
import com.rafptor.parser.audit.PtocaOpcodeReport;

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
    private final Map<String, byte[]> embeddedObjects;
    private final Map<String, String> embeddedObjectKinds;
    private final List<OpaqueSf> opaqueSfs;
    private final List<PtocaOpcodeReport> ptocaReports;
    private ByteAccountingReport byteAccountingReport;

    public AfpDocument(String name) {
        this.name = name;
        this.pages = new ArrayList<>();
        this.resourceReferences = new ArrayList<>();
        this.tags = new HashMap<>();
        this.structuredFields = new ArrayList<>();
        this.embeddedObjects = new HashMap<>();
        this.embeddedObjectKinds = new HashMap<>();
        this.opaqueSfs = new ArrayList<>();
        this.ptocaReports = new ArrayList<>();
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

    /**
     * Register an embedded object resource (typically a JPEG/PNG logo that the
     * MO:DCA composer stored inside a BRS / BFN / BDG envelope). The same
     * resource name can later be referenced by an Include Object to place the
     * image on a page.
     */
    public void putEmbeddedObject(String name, byte[] data, String kind) {
        if (name == null || name.isEmpty() || data == null) {
            return;
        }
        embeddedObjects.put(name, data.clone());
        embeddedObjectKinds.put(name, kind == null ? "UNKNOWN" : kind);
    }

    public byte[] embeddedObject(String name) {
        byte[] data = embeddedObjects.get(name);
        return data == null ? null : data.clone();
    }

    public String embeddedObjectKind(String name) {
        return embeddedObjectKinds.getOrDefault(name, "UNKNOWN");
    }

    public Map<String, byte[]> embeddedObjects() {
        HashMap<String, byte[]> copy = new HashMap<>(embeddedObjects.size());
        for (Map.Entry<String, byte[]> e : embeddedObjects.entrySet()) {
            copy.put(e.getKey(), e.getValue().clone());
        }
        return Collections.unmodifiableMap(copy);
    }

    public void addOpaqueSf(OpaqueSf opaque) {
        if (opaque == null) return;
        opaqueSfs.add(opaque);
    }

    public List<OpaqueSf> opaqueSfs() {
        return Collections.unmodifiableList(opaqueSfs);
    }

    public void addPtocaReport(PtocaOpcodeReport report) {
        if (report == null) return;
        ptocaReports.add(report);
    }

    public List<PtocaOpcodeReport> ptocaReports() {
        return Collections.unmodifiableList(ptocaReports);
    }

    public void setByteAccountingReport(ByteAccountingReport report) {
        this.byteAccountingReport = report;
    }

    public ByteAccountingReport byteAccountingReport() {
        return byteAccountingReport;
    }
}
