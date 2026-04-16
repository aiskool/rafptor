package com.rafptor.converter.transform;

import com.rafptor.parser.model.AfpResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayResolverTest {

    @Test
    void unresolvedOverlayProducesWarning() {
        OverlayResolver r = new OverlayResolver();
        AfpResource res = new AfpResource("MYOVL", AfpResource.ResourceType.PAGE_OVERLAY);
        assertTrue(r.resolve(res).isEmpty());
        assertTrue(r.warningFor(res).contains("MYOVL"));
    }

    @Test
    void filtersOnlyOverlayResources() {
        OverlayResolver r = new OverlayResolver();
        assertFalse(r.warningsFor(java.util.List.of(
                new AfpResource("F", AfpResource.ResourceType.CODED_FONT))).stream()
                .anyMatch(m -> m.contains("F")));
    }
}
