interface Props {
  title: string;
  description: string;
}

export function PlaceholderPage({ title, description }: Props) {
  return (
    <div className="space-y-4">
      <h1 className="font-mono text-xl text-text-primary">{title}</h1>
      <div className="rounded-xl border border-dashed border-white/10 bg-bg-card p-10 text-center text-text-muted">
        {description}
      </div>
    </div>
  );
}
