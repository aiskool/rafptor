package com.rafptor.parser.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Chain of resource libraries tried in order. Typically:
 * InlineResourceLibrary (first, cheapest) → FileSystemResourceLibrary.
 */
public final class CompositeResourceLibrary implements ResourceLibrary {

    private final List<ResourceLibrary> chain;

    public CompositeResourceLibrary(ResourceLibrary... libraries) {
        this.chain = new ArrayList<>();
        for (ResourceLibrary lib : libraries) {
            if (lib != null) chain.add(lib);
        }
    }

    @Override
    public Optional<ResolvedResource> resolve(String name, ResourceType type) {
        for (ResourceLibrary lib : chain) {
            Optional<ResolvedResource> hit = lib.resolve(name, type);
            if (hit.isPresent()) return hit;
        }
        return Optional.empty();
    }
}
