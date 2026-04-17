import { Badge } from "@/components/ui/Badge";
import { Counter } from "@/components/ui/Counter";
import { cn } from "@/lib/utils";

export type ScoreLevel = "high" | "medium" | "low";

export function scoreLevel(score: number): ScoreLevel {
  if (score >= 0.9) return "high";
  if (score >= 0.7) return "medium";
  return "low";
}

const map = {
  high: { variant: "success" as const, label: "" },
  medium: { variant: "warning" as const, label: "" },
  low: { variant: "danger" as const, label: "" },
};

export function ScoreBadge({
  score,
  animated = true,
  className,
}: {
  score: number;
  animated?: boolean;
  className?: string;
}) {
  const level = scoreLevel(score);
  const pct = Math.round(score * 100);
  return (
    <Badge variant={map[level].variant} className={cn("tabular-nums font-semibold", className)}>
      {animated ? <Counter value={pct} duration={900} /> : pct}
      {"\u00A0%"}
    </Badge>
  );
}
