package com.rafptor.api.onboarding;

/** Lifecycle states reported during remote agent deployment. */
public enum OnboardingState {
    CONNECTING,
    AUTHENTICATING,
    DEPLOYING,
    STARTING,
    SCANNING,
    COMPLETED,
    ERROR
}
