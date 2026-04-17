import clsx from "clsx";

interface Props {
  score: number;
}

export function QaScoreBadge({ score }: Props) {
  const bucket = score >= 0.9 ? "high" : score >= 0.7 ? "medium" : "low";
  const label = `${(score * 100).toFixed(1)}%`;
  return (
    <span
      className={clsx(
        "rounded-md px-2 py-0.5 font-mono text-xs",
        bucket === "high" && "bg-score-high/10 text-score-high",
        bucket === "medium" && "bg-score-medium/10 text-score-medium",
        bucket === "low" && "bg-score-low/10 text-score-low",
      )}
    >
      {label}
    </span>
  );
}
