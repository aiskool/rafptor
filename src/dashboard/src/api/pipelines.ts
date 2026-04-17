import { apiClient } from "./client";
import type { PageResponse, PipelineJob } from "./types";

export async function listPipelines(page = 0, size = 20): Promise<PageResponse<PipelineJob>> {
  const response = await apiClient.get<PageResponse<PipelineJob>>("/api/pipelines", {
    params: { page, size },
  });
  return response.data;
}

export async function getPipeline(id: string): Promise<PipelineJob> {
  const response = await apiClient.get<PipelineJob>(`/api/pipelines/${id}`);
  return response.data;
}

export async function createPipeline(): Promise<PipelineJob> {
  const response = await apiClient.post<PipelineJob>("/api/pipelines");
  return response.data;
}

export async function cancelPipeline(id: string): Promise<PipelineJob> {
  const response = await apiClient.post<PipelineJob>(`/api/pipelines/${id}/cancel`);
  return response.data;
}
