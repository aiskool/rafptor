import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getDocument } from "@/api/documents";
import { approveReview, rejectReview } from "@/api/reviews";
import type { Document } from "@/api/types";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorBanner } from "@/components/common/ErrorBanner";
import { QaScoreBadge } from "@/components/documents/QaScoreBadge";
import { StatusBadge } from "@/components/documents/StatusBadge";
import { ZoneHeatmap } from "@/components/review/ZoneHeatmap";
import { ReviewActions } from "@/components/review/ReviewActions";

export function ReviewPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
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

  // Placeholder heatmap — real SSIM grid comes from the validator service
  // exposed via a dedicated endpoint (wired in the next pass).
  const placeholderGrid: number[][] = Array.from({ length: 6 }, (_, row) =>
    Array.from({ length: 8 }, (_, col) => ((row + col) % 3 === 0 ? 0.6 : 0.95)),
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-mono text-xl text-text-primary">{doc.sourceAfpName}</h1>
          <div className="mt-1 text-xs text-text-muted">{doc.pageCount} pages</div>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge status={doc.status} />
          <QaScoreBadge score={doc.compositeScore} />
        </div>
      </div>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <Panel title="AFP reference">
          <Placeholder label="rasterised AFP" />
        </Panel>
        <Panel title="Converted PDF">
          <Placeholder label="PDF preview" />
        </Panel>
        <Panel title="Diff overlay">
          <Placeholder label="per-pixel diff" />
        </Panel>
        <Panel title="Zone heatmap (SSIM 8×6)">
          <ZoneHeatmap grid={placeholderGrid} />
        </Panel>
      </div>
      <ReviewActions
        onApprove={async (comment) => {
          await approveReview(doc.id, comment || undefined);
          navigate("/review");
        }}
        onReject={async (comment) => {
          await rejectReview(doc.id, comment);
          navigate("/review");
        }}
      />
    </div>
  );
}

function Panel({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-xl border border-white/5 bg-bg-card p-4">
      <div className="mb-2 text-xs uppercase tracking-wider text-text-muted">{title}</div>
      {children}
    </div>
  );
}

function Placeholder({ label }: { label: string }) {
  return (
    <div className="flex aspect-[3/4] items-center justify-center rounded-md border border-dashed border-white/10 text-xs text-text-muted">
      {label}
    </div>
  );
}
