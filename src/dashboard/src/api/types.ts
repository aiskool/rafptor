export type QaVerdict = "accepted" | "needs_review" | "rejected";

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

export interface UserProfile {
  id: string;
  email: string;
  tenantId: string;
  roles: string[];
  lastLogin: string;
}

export interface Document {
  id: string;
  tenantId: string;
  pipelineJobId: string | null;
  sourceAfpName: string;
  pdfPath: string | null;
  status: string;
  compositeScore: number;
  visualScore: number;
  structuralScore: number;
  metadataScore: number;
  verdict: QaVerdict | null;
  pageCount: number;
  warnings: string[];
  createdAt: string;
  updatedAt: string;
}

export interface PipelineJob {
  id: string;
  tenantId: string;
  status: string;
  progress: number;
  documentCount: number;
  acceptedCount: number;
  reviewCount: number;
  rejectedCount: number;
  error: string | null;
  createdAt: string;
  finishedAt: string | null;
}

export interface DashboardMetrics {
  totalDocuments: number;
  acceptedCount: number;
  reviewCount: number;
  rejectedCount: number;
  acceptanceRate: number;
  avgCompositeScore: number;
  fidelityScore: number;
}

/**
 * JSON payload returned by {@code GET /api/metrics/overview}. Uses
 * business-neutral snake_case keys — the dashboard normalises to the
 * camelCase {@link DashboardMetrics} shape via {@code fetchOverview}.
 */
export interface MetricsApiResponse {
  total_documents: number;
  accepted_count: number;
  documents_to_check: number;
  rejected_count: number;
  conversion_success_rate: number;
  fidelity_score: number;
  technical_details?: {
    ssim_avg?: number;
    structural_score?: number;
    metadata_score?: number;
  };
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  number: number;
  size: number;
}
