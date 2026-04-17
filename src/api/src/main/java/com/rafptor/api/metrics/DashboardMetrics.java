package com.rafptor.api.metrics;

/**
 * Internal value object carrying all computed metrics before API projection.
 */
public record DashboardMetrics(
        long totalDocuments,
        long acceptedCount,
        long reviewCount,
        long rejectedCount,
        double acceptanceRate,
        double avgCompositeScore,
        double structuralScore,
        double metadataScore
) {
}
