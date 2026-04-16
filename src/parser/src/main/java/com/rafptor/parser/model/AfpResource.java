package com.rafptor.parser.model;

/**
 * A referenced resource (font, overlay, page segment, image, etc.).
 *
 * <p>Rafptor stores the name and type; the actual resource bytes live outside the AST
 * (bundle-local cache or configured resource roots).
 */
public record AfpResource(String name, ResourceType type) {

    public AfpResource {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("resource name must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("resource type must not be null");
        }
    }

    public enum ResourceType {
        CODED_FONT,
        PAGE_OVERLAY,
        PAGE_SEGMENT,
        OBJECT_CONTAINER,
        FORM_DEFINITION,
        PAGE_DEFINITION,
        UNKNOWN
    }
}
