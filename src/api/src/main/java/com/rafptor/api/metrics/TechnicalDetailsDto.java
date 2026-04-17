package com.rafptor.api.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Low-level scoring signals exposed only under a nested {@code technical_details} key.
 */
public record TechnicalDetailsDto(
        @JsonProperty("ssim_avg") double ssimAvg,
        @JsonProperty("structural_score") double structuralScore,
        @JsonProperty("metadata_score") double metadataScore
) {}
