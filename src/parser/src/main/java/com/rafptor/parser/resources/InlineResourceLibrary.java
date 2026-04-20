package com.rafptor.parser.resources;

import com.rafptor.parser.model.AfpDocument;

import java.util.Optional;

/**
 * {@link ResourceLibrary} backed by inline BRG/ERG resources already
 * captured on an {@link AfpDocument}.
 *
 * <p>Covers today: embedded object containers (JPEG / PNG / TIFF). Hooks
 * for inline fonts, overlays and page segments will route through this
 * same adaptor as each implementation lands.
 */
public final class InlineResourceLibrary implements ResourceLibrary {

    private final AfpDocument document;

    public InlineResourceLibrary(AfpDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        this.document = document;
    }

    @Override
    public Optional<ResolvedResource> resolve(String name, ResourceType type) {
        if (name == null || name.isBlank()) return Optional.empty();
        String key = name.trim();
        if (type == ResourceType.OBJECT_CONTAINER || type == ResourceType.TRUETYPE_FONT) {
            byte[] data = document.embeddedObject(key);
            if (data != null && data.length > 0) {
                return Optional.of(new ResolvedResource(key, type, data, "inline://document"));
            }
        }
        return Optional.empty();
    }
}
