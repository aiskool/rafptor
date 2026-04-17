import { useEffect, useState } from "react";
import { listPipelines } from "@/api/pipelines";
import type { PageResponse, PipelineJob } from "@/api/types";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorBanner } from "@/components/common/ErrorBanner";

export function PipelinesPage() {
  const [data, setData] = useState<PageResponse<PipelineJob> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listPipelines()
      .then(setData)
      .catch((err) => setError(err instanceof Error ? err.message : "failed"));
  }, []);

  if (error) return <ErrorBanner message={error} />;
  if (!data) return <LoadingSpinner />;

  return (
    <div className="space-y-4">
      <h1 className="font-mono text-xl text-text-primary">Pipelines</h1>
      <div className="rounded-xl border border-white/5 bg-bg-card">
        <table className="w-full text-left text-sm">
          <thead className="bg-bg-secondary text-xs uppercase text-text-muted">
            <tr>
              <th className="px-4 py-3">Job</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Progress</th>
              <th className="px-4 py-3">Docs</th>
              <th className="px-4 py-3">Created</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {data.content.map((job) => (
              <tr key={job.id}>
                <td className="px-4 py-3 font-mono text-accent">{job.id}</td>
                <td className="px-4 py-3 font-mono text-xs text-text-secondary">{job.status}</td>
                <td className="px-4 py-3 font-mono text-xs text-text-secondary">{job.progress}%</td>
                <td className="px-4 py-3 font-mono text-xs text-text-secondary">{job.documentCount}</td>
                <td className="px-4 py-3 font-mono text-xs text-text-muted">{job.createdAt}</td>
              </tr>
            ))}
            {!data.content.length && (
              <tr>
                <td colSpan={5} className="px-4 py-10 text-center text-text-muted">
                  no pipelines
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
