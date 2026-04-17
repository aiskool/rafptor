import { useParams } from "react-router-dom";
import { Download, Eye } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/Card";
import { ScoreBadge } from "@/components/data/ScoreBadge";
import { t } from "@/i18n";

export default function DocumentViewPage() {
  const { id } = useParams<{ id: string }>();
  const fidelity = 0.94;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <h1 className="text-xl font-semibold text-text-primary">{id}</h1>
          <ScoreBadge score={fidelity} animated={false} />
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" leftIcon={<Eye className="h-4 w-4" />}>
            Comparer
          </Button>
          <Button leftIcon={<Download className="h-4 w-4" />}>
            {t("documents.detail.download")}
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[2fr_1fr]">
        <Card padding="none" className="h-[70vh] overflow-hidden">
          <iframe
            title={`Document ${id}`}
            src={`/api/documents/${id}/preview`}
            className="h-full w-full bg-bg-elevated"
          />
        </Card>
        <div className="flex flex-col gap-4">
          <Card>
            <CardHeader>
              <CardTitle>{t("documents.detail.fidelity")}</CardTitle>
              <CardDescription>Automatique</CardDescription>
            </CardHeader>
            <div className="text-4xl font-semibold tabular-nums">
              {(fidelity * 100).toFixed(1)}%
            </div>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>{t("documents.detail.metadata")}</CardTitle>
            </CardHeader>
            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
              <dt className="text-text-muted">Type</dt>
              <dd className="text-text-primary">Relevé de compte</dd>
              <dt className="text-text-muted">Date</dt>
              <dd className="text-text-primary">15 mars 2024</dd>
              <dt className="text-text-muted">Pages</dt>
              <dd className="text-text-primary">12</dd>
              <dt className="text-text-muted">Référence</dt>
              <dd className="font-mono text-text-primary">FR76 3004…</dd>
            </dl>
          </Card>
        </div>
      </div>
    </div>
  );
}
