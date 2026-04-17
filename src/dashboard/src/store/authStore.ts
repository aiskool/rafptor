import { create } from "zustand";
import type { UserProfile } from "@/api/types";

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserProfile | null;
  setTokens: (access: string, refresh: string) => void;
  setUser: (user: UserProfile | null) => void;
  clear: () => void;
}

/**
 * Tokens are kept in memory only — never in localStorage — so a full
 * page refresh requires a new login. A production build will upgrade the
 * refresh flow to an HTTP-only secure cookie set by the backend.
 */
export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  user: null,
  setTokens: (access, refresh) =>
    set({ accessToken: access, refreshToken: refresh }),
  setUser: (user) => set({ user }),
  clear: () => set({ accessToken: null, refreshToken: null, user: null }),
}));
