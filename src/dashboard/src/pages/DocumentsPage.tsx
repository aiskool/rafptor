import { Link } from "react-router-dom";
import { useDocuments } from "@/hooks/useDocuments";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorBanner } from "@/components/common/ErrorBanner";
import { QaScoreBadge } from "@/components/documents/QaScoreBadge";
import { StatusBadge } from "@/components/documents/StatusBadge";

export function DocumentsPage() {
  const { data, loading, error } = useDocuments();
  if (loading && !data) return <LoadingSpinner />;
  if (error) return <ErrorBanner message={error} />;
  if (!data) return null;

  return (
    <div className="space-y-4">
      <h1 className="font-mono text-xl text-text-primary">Documents</h1>
      <div className="overflow-hidden rounded-xl border border-white/5 bg-bg-card">
        <table className="w-full text-left text-sm">
          <thead className="bg-bg-secondary text-xs uppercase text-text-muted">
            <tr>
              <th className="px-4 py-3">Source</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Composite</th>
              <th className="px-4 py-3">Pages</th>
              <th className="px-4 py-3">Updated</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {data.content.map((doc) => (
              <tr key={doc.id} className="hover:bg-bg-tertiary">
                <td className="px-4 py-3">
                  <Link to={`/documents/${doc.id}`} className="font-mono text-accent hover:underline">
                    {doc.sourceAfpName}
                  </Link>
                </td>
                <td className="px-4 py-3"><StatusBadge status={doc.status} /></td>
                <td className="px-4 py-3"><QaScoreBadge score={doc.compositeScore} /></td>
                <td className="px-4 py-3 font-mono text-xs text-text-secondary">{doc.pageCount}</td>
                <td className="px-4 py-3 font-mono text-xs text-text-muted">{doc.updatedAt}</td>
              </tr>
            ))}
            {!data.content.length && (
              <tr>
                <td colSpan={5} className="px-4 py-10 text-center text-text-muted">
                  no documents
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
