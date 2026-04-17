import { useEffect, useState } from "react";
import { fetchOverview } from "@/api/metrics";
import type { DashboardMetrics } from "@/api/types";

export function useMetrics(refreshIntervalMs = 30_000) {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        const data = await fetchOverview();
        if (active) {
          setMetrics(data);
          setError(null);
        }
      } catch (err) {
        if (active) {
          setError(err instanceof Error ? err.message : "failed to load metrics");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };
    load();
    const t = setInterval(load, refreshIntervalMs);
    return () => {
      active = false;
      clearInterval(t);
    };
  }, [refreshIntervalMs]);

  return { metrics, loading, error };
}
