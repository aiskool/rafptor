import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { reviewQueue } from "@/api/reviews";
import type { Document, PageResponse } from "@/api/types";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorBanner } from "@/components/common/ErrorBanner";
import { QaScoreBadge } from "@/components/documents/QaScoreBadge";

export function ReviewQueuePage() {
  const [data, setData] = useState<PageResponse<Document> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    reviewQueue()
      .then(setData)
      .catch((err) => setError(err instanceof Error ? err.message : "failed"));
  }, []);

  if (error) return <ErrorBanner message={error} />;
  if (!data) return <LoadingSpinner />;

  return (
    <div className="space-y-4">
      <h1 className="font-mono text-xl text-text-primary">Review queue</h1>
      <div className="space-y-2">
        {data.content.map((doc) => (
          <Link
            key={doc.id}
            to={`/review/${doc.id}`}
            className="flex items-center justify-between rounded-xl border border-white/5 bg-bg-card p-4 hover:border-accent/30"
          >
            <div>
              <div className="font-mono text-sm text-text-primary">{doc.sourceAfpName}</div>
              <div className="text-xs text-text-muted">{doc.pageCount} pages · {doc.updatedAt}</div>
            </div>
            <QaScoreBadge score={doc.compositeScore} />
          </Link>
        ))}
        {!data.content.length && (
          <div className="rounded-xl border border-white/5 bg-bg-card p-10 text-center text-text-muted">
            nothing awaiting review
          </div>
        )}
      </div>
    </div>
  );
}
