import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { ErrorBanner } from "@/components/common/ErrorBanner";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [tenantId, setTenantId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(email, password, tenantId);
      navigate("/");
    } catch {
      setError("invalid credentials or tenant");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg-primary">
      <form onSubmit={onSubmit} className="w-full max-w-sm space-y-4 rounded-2xl border border-white/5 bg-bg-card p-8">
        <h1 className="font-mono text-xl text-text-primary">Sign in</h1>
        {error && <ErrorBanner message={error} />}
        <input
          className="w-full rounded-md border border-white/10 bg-bg-secondary p-2 text-sm"
          placeholder="tenant id"
          value={tenantId}
          onChange={(event) => setTenantId(event.target.value)}
          required
        />
        <input
          type="email"
          className="w-full rounded-md border border-white/10 bg-bg-secondary p-2 text-sm"
          placeholder="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />
        <input
          type="password"
          className="w-full rounded-md border border-white/10 bg-bg-secondary p-2 text-sm"
          placeholder="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />
        <button
          type="submit"
          disabled={busy}
          className="w-full rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-40"
        >
          {busy ? "signing in…" : "Sign in"}
        </button>
      </form>
    </div>
  );
}
