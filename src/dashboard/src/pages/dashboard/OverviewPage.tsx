import { Link } from "react-router-dom";
import {
  AlertTriangle,
  Check,
  Clock,
  LayoutDashboard,
  Plus,
  XCircle,
} from "lucide-react";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/Card";
import { Progress } from "@/components/ui/Progress";
import { MetricCard } from "@/components/data/MetricCard";
import { Sparkline } from "@/components/data/Sparkline";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Skeleton } from "@/components/ui/Skeleton";
import { useDashboardMetrics } from "@/hooks/useDashboardMetrics";
import type { DashboardMetrics } from "@/api/types";
import { t } from "@/i18n";

export default function OverviewPage() {
  const { data, loading, error } = useDashboardMetrics();

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-text-primary">Vue d'ensemble</h1>
          <p className="text-sm text-text-secondary">
            {data && data.totalDocuments > 0
              ? "Suivi de vos migrations en cours."
              : "Commencez votre première analyse."}
          </p>
        </div>
        <Link to="/onboarding/welcome">
          <Button size="lg" leftIcon={<Plus className="h-4 w-4" />}>
            Nouvelle analyse
          </Button>
        </Link>
      </div>

      {loading && !data ? <Loading /> : error && !data ? <ErrorState message={error} /> : null}
      {data && data.totalDocuments > 0 ? <Populated metrics={data} /> : data ? <Empty /> : null}
    </div>
  );
}

function Loading() {
  return (
    <div className="flex flex-col gap-4">
      <Skeleton className="h-24 w-full" />
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-32 w-full" />
        ))}
      </div>
      <Skeleton className="h-40 w-full" />
    </div>
  );
}

function ErrorState({ message }: { message: string }) {
  return (
    <Card padding="md" className="border-danger/40 bg-danger-subtle">
      <p className="text-sm text-danger">
        Impossible de charger les métriques : {message}
      </p>
    </Card>
  );
}

function Empty() {
  return (
    <EmptyState
      icon={<LayoutDashboard className="h-6 w-6" />}
      title="Aucune migration pour l'instant"
      description="Connectez votre système et Rafptor détectera automatiquement vos documents à convertir."
      action={
        <Link to="/onboarding/welcome">
          <Button size="lg" leftIcon={<Plus className="h-4 w-4" />}>
            Commencer une analyse
          </Button>
        </Link>
      }
    />
  );
}

function Populated({ metrics }: { metrics: DashboardMetrics }) {
  const converted = metrics.acceptedCount;
  const toCheck = metrics.reviewCount;
  const errors = metrics.rejectedCount;
  const inProgress = Math.max(
    0,
    metrics.totalDocuments - converted - toCheck - errors
  );
  const processed = converted + toCheck + errors;
  const progress = metrics.totalDocuments > 0
    ? Math.round((processed / metrics.totalDocuments) * 100)
    : 0;
  const fidelity = metrics.fidelityScore || metrics.avgCompositeScore || 0;

  return (
    <>
      <Card padding="lg" className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-text-primary">
              Migration en cours
            </h2>
            <p className="text-sm text-text-secondary">
              {progress}% · {t("dashboard.overview.progress").toLowerCase()} · {metrics.totalDocuments} documents au total
            </p>
          </div>
          <span className="rounded-full bg-accent-subtle px-3 py-1 text-xs text-accent">
            {progress >= 100 ? "Terminée" : "En cours"}
          </span>
        </div>
        <Progress value={progress} size="lg" />
      </Card>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <MetricCard
          label={t("dashboard.overview.converted")}
          value={converted}
          accent="success"
          icon={<Check className="h-4 w-4 text-success" />}
        />
        <MetricCard
          label={t("dashboard.overview.inProgress")}
          value={inProgress}
          accent="accent"
          icon={<Clock className="h-4 w-4 text-accent" />}
        />
        <MetricCard
          label={t("dashboard.overview.toCheck")}
          value={toCheck}
          accent="warning"
          icon={<AlertTriangle className="h-4 w-4 text-warning" />}
        />
        <MetricCard
          label={t("dashboard.overview.errors")}
          value={errors}
          accent="danger"
          icon={<XCircle className="h-4 w-4 text-danger" />}
        />
      </div>

      <Card padding="lg" className="flex flex-col gap-3">
        <CardHeader>
          <CardTitle>{t("dashboard.overview.fidelity")}</CardTitle>
          <CardDescription>
            Moyenne sur {metrics.totalDocuments} document{metrics.totalDocuments > 1 ? "s" : ""}
          </CardDescription>
        </CardHeader>
        <div className="flex items-center gap-8">
          <div className="text-5xl font-semibold tabular-nums text-text-primary">
            {(fidelity * 100).toFixed(1)}
            <span className="text-2xl text-text-muted">%</span>
          </div>
          {/* Sparkline left as a follow-up when the backend exposes a time series. */}
          <Sparkline data={[fidelity, fidelity, fidelity]} width={280} height={48} />
        </div>
      </Card>
    </>
  );
}
