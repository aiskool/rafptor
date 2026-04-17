package com.rafptor.api.onboarding;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of the most recent {@link OnboardingProgressEvent} per connection ID.
 */
@Component
public class OnboardingProgressStore {

    private final ConcurrentHashMap<String, OnboardingProgressEvent> store = new ConcurrentHashMap<>();

    /** Stores or replaces the latest event for the given connection. */
    public void put(String connectionId, OnboardingProgressEvent event) {
        store.put(connectionId, event);
    }

    /** Returns the last known event for {@code connectionId}, if any. */
    public Optional<OnboardingProgressEvent> get(String connectionId) {
        return Optional.ofNullable(store.get(connectionId));
    }
}
