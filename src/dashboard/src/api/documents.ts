import { apiClient } from "./client";
import type { Document, PageResponse } from "./types";

export async function listDocuments(params: {
  status?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<Document>> {
  const response = await apiClient.get<PageResponse<Document>>("/api/documents", { params });
  return response.data;
}

export async function getDocument(id: string): Promise<Document> {
  const response = await apiClient.get<Document>(`/api/documents/${id}`);
  return response.data;
}

export async function fetchDocumentPdfBlob(id: string): Promise<Blob> {
  const response = await apiClient.get(`/api/documents/${id}/pdf`, { responseType: "blob" });
  return response.data;
}
