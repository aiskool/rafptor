import type { ReactNode } from "react";
import { TrendingUp, TrendingDown } from "lucide-react";
import { Card } from "@/components/ui/Card";
import { Counter } from "@/components/ui/Counter";
import { Sparkline } from "@/components/data/Sparkline";
import { cn } from "@/lib/utils";

export interface MetricCardProps {
  label: string;
  value: number;
  delta?: number;
  icon?: ReactNode;
  accent?: "neutral" | "accent" | "success" | "warning" | "danger";
  suffix?: string;
  spark?: number[];
  className?: string;
}

const accentMap = {
  neutral: "text-text-primary",
  accent: "text-accent",
  success: "text-success",
  warning: "text-warning",
  danger: "text-danger",
} as const;

export function MetricCard({
  label,
  value,
  delta,
  icon,
  accent = "neutral",
  suffix = "",
  spark,
  className,
}: MetricCardProps) {
  return (
    <Card padding="md" className={cn("flex flex-col gap-3", className)}>
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-2 text-text-secondary">
          {icon}
          <span className="text-sm">{label}</span>
        </div>
        {delta != null && (
          <span
            className={cn(
              "inline-flex items-center gap-1 text-xs",
              delta >= 0 ? "text-success" : "text-danger"
            )}
          >
            {delta >= 0 ? <TrendingUp className="h-3 w-3" /> : <TrendingDown className="h-3 w-3" />}
            {Math.abs(delta)}%
          </span>
        )}
      </div>
      <div className={cn("text-3xl font-semibold tabular-nums", accentMap[accent])}>
        <Counter value={value} />
        {suffix}
      </div>
      {spark && spark.length > 1 && <Sparkline data={spark} width={200} height={32} />}
    </Card>
  );
}
