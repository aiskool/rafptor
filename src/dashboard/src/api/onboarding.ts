import { apiClient } from "./client";

export interface ConnectPayload {
  hostname: string;
  port: number;
  username: string;
  credential: string;
  credentialType: "password" | "ssh_key";
  systemType: "ibmi" | "zos";
}

export interface ConnectResponse {
  connectionId: string;
  status: string;
}

export async function connect(payload: ConnectPayload): Promise<ConnectResponse> {
  const { data } = await apiClient.post<ConnectResponse>("/api/onboarding/connect", payload);
  return data;
}

export interface OnboardingProgress {
  connectionId: string;
  state:
    | "CONNECTING"
    | "AUTHENTICATING"
    | "DEPLOYING"
    | "STARTING"
    | "SCANNING"
    | "COMPLETED"
    | "ERROR";
  message: string;
  timestamp: string;
  counters?: Record<string, number>;
}

export async function getProgress(id: string): Promise<OnboardingProgress> {
  const { data } = await apiClient.get<OnboardingProgress>(`/api/onboarding/${id}/progress`);
  return data;
}

export async function startMigration(connectionId: string): Promise<{ ok: boolean }> {
  const { data } = await apiClient.post<{ ok: boolean }>("/api/onboarding/start-migration", {
    connectionId,
  });
  return data;
}
