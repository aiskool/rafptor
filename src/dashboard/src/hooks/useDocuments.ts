import { useEffect, useState } from "react";
import { listDocuments } from "@/api/documents";
import type { Document, PageResponse } from "@/api/types";

export function useDocuments(status?: string, page = 0, size = 20) {
  const [data, setData] = useState<PageResponse<Document> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    listDocuments({ status, page, size })
      .then((resp) => {
        if (active) {
          setData(resp);
          setError(null);
        }
      })
      .catch((err) => {
        if (active) setError(err instanceof Error ? err.message : "failed");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [status, page, size]);

  return { data, loading, error };
}
