import { cn } from "@/lib/utils";

export type Status = "success" | "warning" | "danger" | "neutral" | "running";

const color: Record<Status, string> = {
  success: "bg-success",
  warning: "bg-warning",
  danger: "bg-danger",
  neutral: "bg-text-muted",
  running: "bg-accent",
};

export function StatusDot({ status, className }: { status: Status; className?: string }) {
  return (
    <span className={cn("relative inline-flex h-2 w-2", className)}>
      {status === "running" && (
        <span className="absolute inset-0 inline-flex animate-pulseRing rounded-full bg-accent/60" />
      )}
      <span className={cn("relative inline-flex h-2 w-2 rounded-full", color[status])} />
    </span>
  );
}
