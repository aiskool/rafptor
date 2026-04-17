import axios, { AxiosError } from "axios";
import { useAuthStore } from "@/store/authStore";

const baseURL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export const apiClient = axios.create({
  baseURL,
  withCredentials: true,
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

let refreshInFlight: Promise<string | null> | null = null;

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (typeof error.config & { _retried?: boolean }) | undefined;
    if (error.response?.status === 401 && original && !original._retried) {
      original._retried = true;
      if (!refreshInFlight) {
        const refresh = useAuthStore.getState().refreshToken;
        if (!refresh) {
          useAuthStore.getState().clear();
          return Promise.reject(error);
        }
        refreshInFlight = axios
          .post<{ accessToken: string; refreshToken: string }>(`${baseURL}/api/auth/refresh`, {
            refreshToken: refresh,
          })
          .then((r) => {
            useAuthStore.getState().setTokens(r.data.accessToken, r.data.refreshToken);
            return r.data.accessToken;
          })
          .catch(() => {
            useAuthStore.getState().clear();
            return null;
          })
          .finally(() => {
            refreshInFlight = null;
          });
      }
      const newAccess = await refreshInFlight;
      if (newAccess) {
        original.headers?.set?.("Authorization", `Bearer ${newAccess}`);
        return apiClient(original);
      }
    }
    return Promise.reject(error);
  },
);
