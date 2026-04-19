import { useEffect, useState } from "react";
import { fetchOverview } from "@/api/metrics";
import type { DashboardMetrics } from "@/api/types";

export interface MetricsState {
  data: DashboardMetrics | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

/**
 * Loads the dashboard metrics once on mount and polls every 30 seconds to
 * keep the view fresh while a migration is running. Falls back to a
 * readable error on failure — the caller is expected to render an
 * empty-state when {@code data} is null.
 */
export function useDashboardMetrics(pollIntervalMs = 30_000): MetricsState {
  const [data, setData] = useState<DashboardMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchOverview()
      .then((payload) => {
        if (!cancelled) {
          setData(payload);
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Erreur de chargement");
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [tick]);

  useEffect(() => {
    if (pollIntervalMs <= 0) return;
    const id = window.setInterval(() => setTick((t) => t + 1), pollIntervalMs);
    return () => window.clearInterval(id);
  }, [pollIntervalMs]);

  return { data, loading, error, refetch: () => setTick((t) => t + 1) };
}
