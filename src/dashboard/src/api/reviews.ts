import { apiClient } from "./client";
import type { Document, PageResponse } from "./types";

export async function reviewQueue(page = 0, size = 20): Promise<PageResponse<Document>> {
  const response = await apiClient.get<PageResponse<Document>>("/api/reviews", {
    params: { page, size },
  });
  return response.data;
}

export async function approveReview(id: string, comment?: string): Promise<void> {
  await apiClient.post(`/api/reviews/${id}/approve`, comment ? { comment } : undefined);
}

export async function rejectReview(id: string, comment: string): Promise<void> {
  await apiClient.post(`/api/reviews/${id}/reject`, { comment });
}
