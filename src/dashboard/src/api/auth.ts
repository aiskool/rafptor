import { apiClient } from "./client";
import type { TokenPair, UserProfile } from "./types";

export async function login(email: string, password: string, tenantId: string): Promise<TokenPair> {
  const response = await apiClient.post<TokenPair>("/api/auth/login", { email, password, tenantId });
  return response.data;
}

export async function refresh(refreshToken: string): Promise<TokenPair> {
  const response = await apiClient.post<TokenPair>("/api/auth/refresh", { refreshToken });
  return response.data;
}

export async function logout(refreshToken: string): Promise<void> {
  await apiClient.post("/api/auth/logout", { refreshToken });
}

export async function me(): Promise<UserProfile> {
  const response = await apiClient.get<UserProfile>("/api/auth/me");
  return response.data;
}
