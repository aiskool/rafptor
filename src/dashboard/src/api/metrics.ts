import { apiClient } from "./client";
import type { DashboardMetrics } from "./types";

export async function fetchOverview(): Promise<DashboardMetrics> {
  const response = await apiClient.get<DashboardMetrics>("/api/metrics/overview");
  return response.data;
}
