import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { getDocument } from "@/api/documents";
import type { Document } from "@/api/types";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorBanner } from "@/components/common/ErrorBanner";
import { QaScoreBadge } from "@/components/documents/QaScoreBadge";
import { StatusBadge } from "@/components/documents/StatusBadge";

export function DocumentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [doc, setDoc] = useState<Document | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    getDocument(id)
      .then(setDoc)
      .catch((err) => setError(err instanceof Error ? err.message : "failed"));
  }, [id]);

  if (error) return <ErrorBanner message={error} />;
  if (!doc) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="font-mono text-xl text-text-primary">{doc.sourceAfpName}</h1>
        <div className="flex items-center gap-3">
          <StatusBadge status={doc.status} />
          <QaScoreBadge score={doc.compositeScore} />
        </div>
      </div>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <ScoreCard label="Visual (SSIM)" value={doc.visualScore} />
        <ScoreCard label="Structural" value={doc.structuralScore} />
        <ScoreCard label="Metadata" value={doc.metadataScore} />
      </div>
      {doc.warnings.length > 0 && (
        <div className="rounded-xl border border-warning/20 bg-warning/10 p-4 text-sm text-warning">
          <div className="mb-2 font-mono text-xs uppercase tracking-wide">Warnings</div>
          <ul className="list-disc pl-5">
            {doc.warnings.map((w, idx) => (
              <li key={idx}>{w}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function ScoreCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-xl border border-white/5 bg-bg-card p-5">
      <div className="text-xs uppercase tracking-wider text-text-muted">{label}</div>
      <div className="mt-2 font-mono text-2xl text-text-primary">{(value * 100).toFixed(1)}%</div>
    </div>
  );
}
