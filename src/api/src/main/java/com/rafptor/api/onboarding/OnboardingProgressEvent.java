package com.rafptor.api.onboarding;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable event broadcast over WebSocket to track deployment progress.
 *
 * @param connectionId  unique identifier for this deployment attempt
 * @param state         current lifecycle state
 * @param message       human-readable status description
 * @param timestamp     UTC instant at which the event was created
 * @param counters      optional numeric counters (e.g. files discovered)
 */
public record OnboardingProgressEvent(
        String connectionId,
        OnboardingState state,
        String message,
        Instant timestamp,
        Map<String, Object> counters
) {

    /** Convenience factory with empty counters. */
    public static OnboardingProgressEvent of(String connectionId, OnboardingState state, String message) {
        return new OnboardingProgressEvent(connectionId, state, message, Instant.now(), Map.of());
    }
}
