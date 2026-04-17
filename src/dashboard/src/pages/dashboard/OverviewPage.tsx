import { Link } from "react-router-dom";
import { AlertTriangle, Check, Clock, XCircle, ArrowRight, FileText } from "lucide-react";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/Card";
import { Progress } from "@/components/ui/Progress";
import { MetricCard } from "@/components/data/MetricCard";
import { Sparkline } from "@/components/data/Sparkline";
import { ScoreBadge } from "@/components/data/ScoreBadge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { t } from "@/i18n";

// Demo data; real data comes from /api/metrics/overview + WebSocket updates.
const demo = {
  projectName: "Relevés T1 2024",
  progress: 78,
  converted: 973,
  inProgress: 241,
  toCheck: 28,
  errors: 5,
  fidelity: 0.962,
  spark: [0.93, 0.94, 0.95, 0.93, 0.96, 0.97, 0.95, 0.96, 0.96, 0.97, 0.96, 0.96],
  review: [
    { id: "REL_00847", name: "REL_00847", score: 0.87, pages: 12 },
    { id: "FAC_01203", name: "FAC_01203", score: 0.82, pages: 4 },
    { id: "REL_00912", name: "REL_00912", score: 0.79, pages: 8 },
  ],
};

export default function OverviewPage() {
  return (
    <div className="flex flex-col gap-6">
      <Card padding="lg" className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-semibold text-text-primary">
              Migration « {demo.projectName} »
            </h1>
            <p className="text-sm text-text-secondary">
              {demo.progress}% · {t("dashboard.overview.progress").toLowerCase()}
            </p>
          </div>
          <span className="rounded-full bg-accent-subtle px-3 py-1 text-xs text-accent">En cours</span>
        </div>
        <Progress value={demo.progress} size="lg" />
      </Card>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <MetricCard
          label={t("dashboard.overview.converted")}
          value={demo.converted}
          accent="success"
          icon={<Check className="h-4 w-4 text-success" />}
        />
        <MetricCard
          label={t("dashboard.overview.inProgress")}
          value={demo.inProgress}
          accent="accent"
          icon={<Clock className="h-4 w-4 text-accent" />}
        />
        <MetricCard
          label={t("dashboard.overview.toCheck")}
          value={demo.toCheck}
          accent="warning"
          icon={<AlertTriangle className="h-4 w-4 text-warning" />}
        />
        <MetricCard
          label={t("dashboard.overview.errors")}
          value={demo.errors}
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
            {(demo.fidelity * 100).toFixed(1)}
            <span className="text-2xl text-text-muted">%</span>
          </div>
          <Sparkline data={demo.spark} width={280} height={48} />
        </div>
      </Card>

      <Card padding="md">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-base font-medium text-text-primary">
            {t("dashboard.overview.documentsToCheck")}
          </h3>
          <Link
            to="/review"
            className="text-xs text-accent hover:text-accent-hover inline-flex items-center gap-1"
          >
            Voir tout <ArrowRight className="h-3 w-3" />
          </Link>
        </div>
        {demo.review.length === 0 ? (
          <EmptyState
            icon={<Check className="h-5 w-5" />}
            title={t("dashboard.overview.noReview")}
          />
        ) : (
          <ul className="flex flex-col divide-y divide-border">
            {demo.review.map((d) => (
              <li
                key={d.id}
                className="flex items-center gap-3 py-2.5 first:pt-0 last:pb-0"
              >
                <FileText className="h-4 w-4 text-text-muted" />
                <span className="flex-1 truncate text-sm text-text-primary">{d.name}</span>
                <ScoreBadge score={d.score} animated={false} />
                <span className="w-16 text-right text-xs text-text-muted tabular-nums">
                  {d.pages} p.
                </span>
                <Link to={`/review/${d.id}`}>
                  <Button size="sm" variant="ghost" rightIcon={<ArrowRight className="h-3.5 w-3.5" />}>
                    Vérifier
                  </Button>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
