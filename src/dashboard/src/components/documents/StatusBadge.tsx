import clsx from "clsx";

interface Props {
  status: string;
}

const STATUS_COLOURS: Record<string, string> = {
  ACCEPTED: "bg-success/10 text-success",
  REVIEW: "bg-warning/10 text-warning",
  REJECTED: "bg-danger/10 text-danger",
  CONVERTED: "bg-info/10 text-info",
  VALIDATED: "bg-info/10 text-info",
  PARSED: "bg-text-muted/10 text-text-muted",
  RECEIVED: "bg-text-muted/10 text-text-muted",
};

export function StatusBadge({ status }: Props) {
  const colour = STATUS_COLOURS[status] ?? "bg-text-muted/10 text-text-muted";
  return (
    <span className={clsx("rounded-md px-2 py-0.5 font-mono text-xs uppercase", colour)}>
      {status.toLowerCase()}
    </span>
  );
}
