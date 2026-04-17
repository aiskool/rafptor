import { create } from "zustand";
import type { SystemType, CredentialType } from "@/components/onboarding/CredentialForm";

export type OnboardingStep =
  | "welcome"
  | "connect"
  | "scanning"
  | "review"
  | "started";

export interface ScanCounters {
  documents: number;
  fonts: number;
  templates: number;
  bytes: number;
  pages?: number;
  durationMs?: number;
}

interface OnboardingState {
  step: OnboardingStep;
  connectionId: string | null;
  systemType: SystemType | null;
  hostname: string;
  credentialType: CredentialType | null;
  counters: ScanCounters;
  scanCompleted: boolean;
  outputFormat: "pdf" | "pdfa";
  quality: "standard" | "high";
  setStep: (step: OnboardingStep) => void;
  setConnection: (id: string, hostname: string, systemType: SystemType, credentialType: CredentialType) => void;
  updateCounters: (c: Partial<ScanCounters>) => void;
  setScanCompleted: (v: boolean) => void;
  setOutputFormat: (v: "pdf" | "pdfa") => void;
  setQuality: (v: "standard" | "high") => void;
  reset: () => void;
}

export const useOnboardingStore = create<OnboardingState>((set) => ({
  step: "welcome",
  connectionId: null,
  systemType: null,
  hostname: "",
  credentialType: null,
  counters: { documents: 0, fonts: 0, templates: 0, bytes: 0 },
  scanCompleted: false,
  outputFormat: "pdfa",
  quality: "high",
  setStep: (step) => set({ step }),
  setConnection: (id, hostname, systemType, credentialType) =>
    set({ connectionId: id, hostname, systemType, credentialType }),
  updateCounters: (c) => set((s) => ({ counters: { ...s.counters, ...c } })),
  setScanCompleted: (v) => set({ scanCompleted: v }),
  setOutputFormat: (outputFormat) => set({ outputFormat }),
  setQuality: (quality) => set({ quality }),
  reset: () =>
    set({
      step: "welcome",
      connectionId: null,
      systemType: null,
      hostname: "",
      credentialType: null,
      counters: { documents: 0, fonts: 0, templates: 0, bytes: 0 },
      scanCompleted: false,
    }),
}));
