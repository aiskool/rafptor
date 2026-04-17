import { LogOut } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";

export function TopBar() {
  const { user, logout } = useAuth();
  return (
    <header className="flex items-center justify-between border-b border-white/5 bg-bg-primary px-6 py-4">
      <div className="font-mono text-xs uppercase tracking-wider text-text-muted">
        tenant <span className="text-text-secondary">{user?.tenantId ?? "—"}</span>
      </div>
      <div className="flex items-center gap-4">
        <span className="text-sm text-text-secondary">{user?.email}</span>
        <button
          onClick={logout}
          className="flex items-center gap-1 rounded-md border border-white/10 px-3 py-1 text-xs text-text-secondary hover:text-text-primary"
        >
          <LogOut size={14} />
          Logout
        </button>
      </div>
    </header>
  );
}
