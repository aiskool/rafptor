import { create } from "zustand";
import { persist } from "zustand/middleware";
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
 * Persisted to localStorage so a page refresh keeps the session. A production
 * build will upgrade the refresh flow to an HTTP-only secure cookie set by
 * the backend and drop the refreshToken from the persisted state.
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setTokens: (access, refresh) => set({ accessToken: access, refreshToken: refresh }),
      setUser: (user) => set({ user }),
      clear: () => set({ accessToken: null, refreshToken: null, user: null }),
    }),
    { name: "rafptor-auth" }
  )
);
