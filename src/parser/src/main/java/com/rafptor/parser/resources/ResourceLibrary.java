package com.rafptor.parser.resources;

import java.util.Optional;

/**
 * Lookup service for AFP resources referenced by name from a document
 * (overlays, page segments, coded fonts, code pages, Object Containers,
 * Form Definitions, Page Definitions, embedded TrueType fonts).
 *
 * <p>A {@code ResourceLibrary} is intentionally lightweight: the parser
 * calls {@link #resolve} exactly once per Include record and never cares
 * about where the bytes come from. Implementations chain
 * {@link InlineResourceLibrary} (BRG/ERG resources inside the AFP)
 * before {@link FileSystemResourceLibrary} (a directory the operator
 * passes via {@code --resources}), composed in {@link CompositeResourceLibrary}.
 */
public interface ResourceLibrary {

    Optional<ResolvedResource> resolve(String name, ResourceType type);

    /** A library that always returns empty — useful as a default. */
    ResourceLibrary EMPTY = (name, type) -> Optional.empty();
}
