import { Link } from "react-router-dom";
import {
  AlertTriangle,
  ArrowRight,
  Check,
  Clock,
  FileText,
  LayoutDashboard,
  Plus,
  XCircle,
} from "lucide-react";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/Card";
import { Progress } from "@/components/ui/Progress";
import { MetricCard } from "@/components/data/MetricCard";
import { Sparkline } from "@/components/data/Sparkline";
import { ScoreBadge } from "@/components/data/ScoreBadge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { t } from "@/i18n";

interface ReviewItem {
  id: string;
  name: string;
  score: number;
  pages: number;
}

interface Migration {
  projectName: string;
  progress: number;
  converted: number;
  inProgress: number;
  toCheck: number;
  errors: number;
  fidelity: number;
  spark: number[];
  review: ReviewItem[];
}

// Swap for real data once /api/metrics/overview is wired.
const migrations: Migration[] = [];

export default function OverviewPage() {
  const current = migrations[0];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-text-primary">Vue d'ensemble</h1>
          <p className="text-sm text-text-secondary">
            {current ? "Suivi de vos migrations en cours." : "Commencez votre première analyse."}
          </p>
        </div>
        <Link to="/onboarding/welcome">
          <Button size="lg" leftIcon={<Plus className="h-4 w-4" />}>
            Nouvelle analyse
          </Button>
        </Link>
      </div>

      {current ? <Populated migration={current} /> : <Empty />}
    </div>
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

function Populated({ migration }: { migration: Migration }) {
  return (
    <>
      <Card padding="lg" className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-text-primary">
              Migration « {migration.projectName} »
            </h2>
            <p className="text-sm text-text-secondary">
              {migration.progress}% · {t("dashboard.overview.progress").toLowerCase()}
            </p>
          </div>
          <span className="rounded-full bg-accent-subtle px-3 py-1 text-xs text-accent">
            En cours
          </span>
        </div>
        <Progress value={migration.progress} size="lg" />
      </Card>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <MetricCard
          label={t("dashboard.overview.converted")}
          value={migration.converted}
          accent="success"
          icon={<Check className="h-4 w-4 text-success" />}
        />
        <MetricCard
          label={t("dashboard.overview.inProgress")}
          value={migration.inProgress}
          accent="accent"
          icon={<Clock className="h-4 w-4 text-accent" />}
        />
        <MetricCard
          label={t("dashboard.overview.toCheck")}
          value={migration.toCheck}
          accent="warning"
          icon={<AlertTriangle className="h-4 w-4 text-warning" />}
        />
        <MetricCard
          label={t("dashboard.overview.errors")}
          value={migration.errors}
          accent="danger"
          icon={<XCircle className="h-4 w-4 text-danger" />}
        />
      </div>

      <Card padding="lg" className="flex flex-col gap-3">
        <CardHeader>
          <CardTitle>{t("dashboard.overview.fidelity")}</CardTitle>
          <CardDescription>30 derniers documents</CardDescription>
        </CardHeader>
        <div className="flex items-center gap-8">
          <div className="text-5xl font-semibold tabular-nums text-text-primary">
            {(migration.fidelity * 100).toFixed(1)}
            <span className="text-2xl text-text-muted">%</span>
          </div>
          <Sparkline data={migration.spark} width={280} height={48} />
        </div>
      </Card>

      <Card padding="md">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-base font-medium text-text-primary">
            {t("dashboard.overview.documentsToCheck")}
          </h3>
          <Link
            to="/review"
            className="inline-flex items-center gap-1 text-xs text-accent hover:text-accent-hover"
          >
            Voir tout <ArrowRight className="h-3 w-3" />
          </Link>
        </div>
        {migration.review.length === 0 ? (
          <EmptyState
            icon={<Check className="h-5 w-5" />}
            title={t("dashboard.overview.noReview")}
          />
        ) : (
          <ul className="flex flex-col divide-y divide-border">
            {migration.review.map((d) => (
              <li key={d.id} className="flex items-center gap-3 py-2.5 first:pt-0 last:pb-0">
                <FileText className="h-4 w-4 text-text-muted" />
                <span className="flex-1 truncate text-sm text-text-primary">{d.name}</span>
                <ScoreBadge score={d.score} animated={false} />
                <span className="w-16 text-right text-xs text-text-muted tabular-nums">
                  {d.pages} p.
                </span>
                <Link to={`/review/${d.id}`}>
                  <Button
                    size="sm"
                    variant="ghost"
                    rightIcon={<ArrowRight className="h-3.5 w-3.5" />}
                  >
                    Vérifier
                  </Button>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </>
  );
}
