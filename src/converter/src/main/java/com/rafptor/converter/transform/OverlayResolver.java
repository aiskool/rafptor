package com.rafptor.converter.transform;

import com.rafptor.converter.ir.IrOverlay;
import com.rafptor.parser.model.AfpResource;

import java.util.List;
import java.util.Optional;

/**
 * Resolves a page-overlay reference to a list of IR elements. In this release
 * the resource library is not wired yet (Module 3 collector delivers the
 * overlay files later). The resolver therefore always returns empty and
 * exposes a warning string for the caller to register on the
 * {@link com.rafptor.converter.ConversionResult}.
 */
public final class OverlayResolver {

    public Optional<IrOverlay> resolve(AfpResource overlay) {
        return Optional.empty();
    }

    public String warningFor(AfpResource overlay) {
        return "Overlay not found in resource library: " + overlay.name();
    }

    /** Convenience for callers that need to process every overlay of a page. */
    public List<String> warningsFor(List<AfpResource> overlays) {
        return overlays.stream()
                .filter(r -> r.type() == AfpResource.ResourceType.PAGE_OVERLAY)
                .map(this::warningFor)
                .toList();
    }
}
