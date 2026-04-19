import { apiClient } from "./client";
import type { DashboardMetrics, MetricsApiResponse } from "./types";

/**
 * Fetch {@code /api/metrics/overview} and normalise the snake_case payload
 * into the dashboard's camelCase {@link DashboardMetrics} shape.
 */
export async function fetchOverview(): Promise<DashboardMetrics> {
  const response = await apiClient.get<MetricsApiResponse>("/api/metrics/overview");
  const body = response.data;
  return {
    totalDocuments: body.total_documents ?? 0,
    acceptedCount: body.accepted_count ?? 0,
    reviewCount: body.documents_to_check ?? 0,
    rejectedCount: body.rejected_count ?? 0,
    acceptanceRate: body.conversion_success_rate ?? 0,
    avgCompositeScore: body.technical_details?.ssim_avg ?? body.fidelity_score ?? 0,
    fidelityScore: body.fidelity_score ?? 0,
  };
}
