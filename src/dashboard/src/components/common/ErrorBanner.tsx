interface Props {
  message: string;
}

export function ErrorBanner({ message }: Props) {
  return (
    <div className="rounded-lg border border-danger/20 bg-danger/10 p-4 text-sm text-danger">
      {message}
    </div>
  );
}
