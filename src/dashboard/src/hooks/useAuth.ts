import { useCallback } from "react";
import { useAuthStore } from "@/store/authStore";
import * as authApi from "@/api/auth";

export function useAuth() {
  const { accessToken, user, setTokens, setUser, clear } = useAuthStore();

  const login = useCallback(
    async (email: string, password: string, tenantId: string) => {
      const tokens = await authApi.login(email, password, tenantId);
      setTokens(tokens.accessToken, tokens.refreshToken);
      const profile = await authApi.me();
      setUser(profile);
    },
    [setTokens, setUser],
  );

  const logout = useCallback(async () => {
    const refresh = useAuthStore.getState().refreshToken;
    if (refresh) {
      try {
        await authApi.logout(refresh);
      } catch {
        /* best effort */
      }
    }
    clear();
  }, [clear]);

  return {
    isAuthenticated: Boolean(accessToken && user),
    user,
    login,
    logout,
  };
}
