import { create } from "zustand";
import { persist } from "zustand/middleware";

interface SettingsState {
  expertMode: boolean;
  acceptThreshold: number;
  reviewThreshold: number;
  notifyOnComplete: boolean;
  notifyOnError: boolean;
  setExpertMode: (v: boolean) => void;
  setAcceptThreshold: (v: number) => void;
  setReviewThreshold: (v: number) => void;
  setNotifyOnComplete: (v: boolean) => void;
  setNotifyOnError: (v: boolean) => void;
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      expertMode: false,
      acceptThreshold: 0.9,
      reviewThreshold: 0.7,
      notifyOnComplete: true,
      notifyOnError: true,
      setExpertMode: (v) => set({ expertMode: v }),
      setAcceptThreshold: (v) => set({ acceptThreshold: v }),
      setReviewThreshold: (v) => set({ reviewThreshold: v }),
      setNotifyOnComplete: (v) => set({ notifyOnComplete: v }),
      setNotifyOnError: (v) => set({ notifyOnError: v }),
    }),
    { name: "rafptor-settings" }
  )
);
