package com.rafptor.api.review;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReviewDecisionTest {

    @Test
    void constructorPopulatesAllFields() {
        ReviewDecision decision = new ReviewDecision(
                "t1", "doc-1", "reviewer-1", ReviewDecision.Outcome.APPROVED, "ok");
        assertEquals("t1", decision.getTenantId());
        assertEquals(ReviewDecision.Outcome.APPROVED, decision.getOutcome());
        assertNotNull(decision.getDecidedAt());
    }
}
