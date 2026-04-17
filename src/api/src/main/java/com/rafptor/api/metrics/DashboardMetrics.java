package com.rafptor.api.metrics;

public record DashboardMetrics(
        long totalDocuments,
        long acceptedCount,
        long reviewCount,
        long rejectedCount,
        double acceptanceRate,
        double avgCompositeScore
) {
}
