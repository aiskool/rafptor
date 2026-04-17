import { Timeline, type TimelineStep } from "@/components/data/Timeline";
import { Progress } from "@/components/ui/Progress";

export interface ConnectionStep {
  id: string;
  label: string;
  description?: string;
  status: TimelineStep["status"];
}

export interface ConnectionProgressProps {
  steps: ConnectionStep[];
}

export function ConnectionProgress({ steps }: ConnectionProgressProps) {
  const done = steps.filter((s) => s.status === "done").length;
  const running = steps.some((s) => s.status === "running");
  const pct = (done / steps.length) * 100 + (running ? 100 / steps.length / 2 : 0);

  return (
    <div className="flex flex-col gap-6">
      <Timeline steps={steps} />
      <Progress value={Math.round(pct)} size="md" />
    </div>
  );
}
