interface Props {
  label: string;
  value: string;
  hint?: string;
}

export function MetricCard({ label, value, hint }: Props) {
  return (
    <div className="rounded-xl border border-white/5 bg-bg-card p-5">
      <div className="text-xs uppercase tracking-wider text-text-muted">{label}</div>
      <div className="mt-3 font-mono text-2xl text-text-primary">{value}</div>
      {hint && <div className="mt-1 text-xs text-text-secondary">{hint}</div>}
    </div>
  );
}
