import { ChevronRight, LogOut } from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/utils";

const CRUMBS: Record<string, string> = {
  dashboard: "Vue d'ensemble",
  documents: "Documents",
  review: "Vérification",
  settings: "Paramètres",
  onboarding: "Configuration",
};

export function TopBar() {
  const { user, logout } = useAuth();
  const { pathname } = useLocation();
  const segments = pathname.split("/").filter(Boolean);

  return (
    <header className="flex h-16 items-center justify-between border-b border-border bg-bg-base/80 px-6 backdrop-blur-sm">
      <nav aria-label="Fil d'Ariane" className="flex items-center gap-1.5 text-sm text-text-muted">
        {segments.length === 0 ? (
          <span>Accueil</span>
        ) : (
          segments.map((s, i) => {
            const label = CRUMBS[s] ?? s;
            const href = "/" + segments.slice(0, i + 1).join("/");
            const last = i === segments.length - 1;
            return (
              <span key={href} className="flex items-center gap-1.5">
                {i > 0 && <ChevronRight className="h-3.5 w-3.5" />}
                {last ? (
                  <span className="text-text-primary">{label}</span>
                ) : (
                  <Link to={href} className="hover:text-text-secondary transition-colors">
                    {label}
                  </Link>
                )}
              </span>
            );
          })
        )}
      </nav>
      <div className="flex items-center gap-3">
        {user && (
          <div className={cn("flex items-center gap-2 text-sm text-text-secondary")}>
            <Avatar name={user.email || "User"} size={28} />
            <span className="hidden sm:block">{user.email}</span>
          </div>
        )}
        <Button
          variant="ghost"
          size="sm"
          leftIcon={<LogOut className="h-3.5 w-3.5" />}
          onClick={logout}
        >
          Se déconnecter
        </Button>
      </div>
    </header>
  );
}
