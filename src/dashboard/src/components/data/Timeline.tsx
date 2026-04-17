import type { ReactNode } from "react";
import { Check, Circle, Loader2, X } from "lucide-react";
import { cn } from "@/lib/utils";

export type TimelineStepStatus = "pending" | "running" | "done" | "error";

export interface TimelineStep {
  id: string;
  label: string;
  description?: string;
  status: TimelineStepStatus;
  icon?: ReactNode;
}

export function Timeline({ steps, className }: { steps: TimelineStep[]; className?: string }) {
  return (
    <ol className={cn("flex flex-col", className)}>
      {steps.map((step, i) => (
        <li key={step.id} className="flex gap-4">
          <div className="flex flex-col items-center">
            <StepIcon status={step.status} />
            {i < steps.length - 1 && (
              <div
                className={cn(
                  "mt-1 h-10 w-px",
                  step.status === "done" ? "bg-accent" : "bg-border"
                )}
              />
            )}
          </div>
          <div className="pb-6">
            <div
              className={cn(
                "text-sm font-medium transition-colors",
                step.status === "pending" ? "text-text-muted" : "text-text-primary"
              )}
            >
              {step.label}
            </div>
            {step.description && (
              <div className="mt-0.5 text-xs text-text-secondary">{step.description}</div>
            )}
          </div>
        </li>
      ))}
    </ol>
  );
}

function StepIcon({ status }: { status: TimelineStepStatus }) {
  const base = "flex h-6 w-6 items-center justify-center rounded-full border";
  if (status === "done")
    return (
      <span className={cn(base, "border-accent bg-accent text-white")}>
        <Check className="h-3.5 w-3.5" />
      </span>
    );
  if (status === "running")
    return (
      <span className={cn(base, "border-accent bg-accent/10 text-accent")}>
        <Loader2 className="h-3.5 w-3.5 animate-spin" />
      </span>
    );
  if (status === "error")
    return (
      <span className={cn(base, "border-danger bg-danger/10 text-danger")}>
        <X className="h-3.5 w-3.5" />
      </span>
    );
  return (
    <span className={cn(base, "border-border bg-bg-elevated text-text-muted")}>
      <Circle className="h-2 w-2 fill-current" />
    </span>
  );
}
