import { useState } from "react";

interface Props {
  onApprove: (comment: string) => Promise<void>;
  onReject: (comment: string) => Promise<void>;
  disabled?: boolean;
}

export function ReviewActions({ onApprove, onReject, disabled }: Props) {
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const approve = async () => {
    setBusy(true);
    setError(null);
    try {
      await onApprove(comment);
      setComment("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "approval failed");
    } finally {
      setBusy(false);
    }
  };

  const reject = async () => {
    if (!comment.trim()) {
      setError("a comment is required when rejecting");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onReject(comment);
      setComment("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "rejection failed");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-3 rounded-xl border border-white/5 bg-bg-card p-4">
      <textarea
        className="w-full rounded-md border border-white/10 bg-bg-secondary p-2 font-mono text-sm text-text-primary"
        rows={3}
        placeholder="Comment (mandatory when rejecting)…"
        value={comment}
        onChange={(event) => setComment(event.target.value)}
        disabled={disabled || busy}
      />
      {error && <div className="text-xs text-danger">{error}</div>}
      <div className="flex gap-3">
        <button
          className="rounded-md bg-success/20 px-4 py-2 text-sm text-success hover:bg-success/30 disabled:opacity-40"
          onClick={approve}
          disabled={disabled || busy}
        >
          Approve
        </button>
        <button
          className="rounded-md border border-danger/40 px-4 py-2 text-sm text-danger hover:bg-danger/10 disabled:opacity-40"
          onClick={reject}
          disabled={disabled || busy}
        >
          Reject
        </button>
      </div>
    </div>
  );
}
