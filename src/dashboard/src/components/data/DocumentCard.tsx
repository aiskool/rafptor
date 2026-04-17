import { Link } from "react-router-dom";
import { FileText } from "lucide-react";
import { Card } from "@/components/ui/Card";
import { ScoreBadge } from "@/components/data/ScoreBadge";
import { formatRelativeTime } from "@/lib/utils";

export interface DocumentCardData {
  id: string;
  name: string;
  pageCount: number;
  fidelityScore: number;
  thumbnailUrl?: string;
  createdAt: string;
}

export function DocumentCard({ doc }: { doc: DocumentCardData }) {
  return (
    <Link to={`/documents/${doc.id}`} className="block">
      <Card padding="none" className="overflow-hidden transition-transform duration-base hover:-translate-y-0.5">
        <div className="aspect-[5/7] w-full bg-bg-elevated flex items-center justify-center overflow-hidden border-b border-border">
          {doc.thumbnailUrl ? (
            <img
              src={doc.thumbnailUrl}
              alt=""
              loading="lazy"
              className="h-full w-full object-cover"
            />
          ) : (
            <FileText className="h-10 w-10 text-text-muted" />
          )}
        </div>
        <div className="flex flex-col gap-1.5 p-4">
          <div className="flex items-center justify-between gap-2">
            <span className="truncate text-sm font-medium text-text-primary">{doc.name}</span>
            <ScoreBadge score={doc.fidelityScore} animated={false} />
          </div>
          <div className="flex items-center justify-between text-xs text-text-muted">
            <span>
              {doc.pageCount} {doc.pageCount > 1 ? "pages" : "page"}
            </span>
            <span>{formatRelativeTime(doc.createdAt)}</span>
          </div>
        </div>
      </Card>
    </Link>
  );
}
