package com.rafptor.api.pipeline;

public enum PipelineStatus {
    PENDING,
    PARSING,
    CONVERTING,
    VALIDATING,
    DONE,
    FAILED,
    CANCELLED
}
