package com.rafptor.api.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Public API response for dashboard metrics — uses business-neutral field names.
 */
public record MetricsDto(
        @JsonProperty("total_documents") long totalDocuments,
        @JsonProperty("accepted_count") long acceptedCount,
        @JsonProperty("documents_to_check") long documentsToCheck,
        @JsonProperty("rejected_count") long rejectedCount,
        @JsonProperty("conversion_success_rate") double conversionSuccessRate,
        @JsonProperty("fidelity_score") double fidelityScore,
        @JsonProperty("technical_details") TechnicalDetailsDto technicalDetails
) {

    /** Maps from the internal {@link DashboardMetrics} record. */
    public static MetricsDto from(DashboardMetrics m) {
        TechnicalDetailsDto tech = new TechnicalDetailsDto(
                m.avgCompositeScore(),
                m.structuralScore(),
                m.metadataScore()
        );
        return new MetricsDto(
                m.totalDocuments(),
                m.acceptedCount(),
                m.reviewCount(),
                m.rejectedCount(),
                m.acceptanceRate(),
                m.avgCompositeScore(),
                tech
        );
    }
}
