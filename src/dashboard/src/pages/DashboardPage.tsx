import { useMetrics } from "@/hooks/useMetrics";
import { MetricCard } from "@/components/dashboard/MetricCard";
import { LoadingSpinner } from "@/components/common/LoadingSpinner";
import { ErrorBanner } from "@/components/common/ErrorBanner";

export function DashboardPage() {
  const { metrics, loading, error } = useMetrics();

  if (loading && !metrics) {
    return <LoadingSpinner />;
  }
  if (error) {
    return <ErrorBanner message={error} />;
  }
  if (!metrics) {
    return null;
  }

  return (
    <div className="space-y-6">
      <h1 className="font-mono text-xl text-text-primary">Dashboard</h1>
      <div className="grid grid-cols-2 gap-4 md:grid-cols-5">
        <MetricCard label="Documents" value={metrics.totalDocuments.toString()} />
        <MetricCard
          label="Accepted"
          value={`${(metrics.acceptanceRate * 100).toFixed(1)}%`}
          hint={`${metrics.acceptedCount} accepted`}
        />
        <MetricCard label="Review queue" value={metrics.reviewCount.toString()} />
        <MetricCard label="Avg SSIM" value={metrics.avgCompositeScore.toFixed(3)} />
        <MetricCard label="Rejected" value={metrics.rejectedCount.toString()} />
      </div>
    </div>
  );
}
