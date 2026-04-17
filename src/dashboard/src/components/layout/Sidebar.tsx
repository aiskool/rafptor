import { NavLink } from "react-router-dom";
import { CheckCircle2, FileText, HelpCircle, LayoutDashboard, Settings } from "lucide-react";
import { cn } from "@/lib/utils";
import { Divider } from "@/components/ui/Divider";

const NAV = [
  { to: "/dashboard", label: "Vue d'ensemble", icon: LayoutDashboard },
  { to: "/documents", label: "Documents", icon: FileText },
  { to: "/review", label: "Vérification", icon: CheckCircle2 },
  { to: "/settings", label: "Paramètres", icon: Settings },
];

export function Sidebar() {
  return (
    <aside className="hidden h-full w-60 flex-col border-r border-border bg-bg-raised md:flex">
      <div className="flex h-16 items-center px-6">
        <span className="text-lg font-bold tracking-tight">
          <span className="text-text-primary">raf</span>
          <span className="text-accent">ptor</span>
        </span>
      </div>
      <Divider />
      <nav className="flex flex-1 flex-col gap-0.5 p-3">
        {NAV.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors duration-base",
                isActive
                  ? "bg-accent-subtle text-accent"
                  : "text-text-secondary hover:bg-bg-elevated hover:text-text-primary"
              )
            }
          >
            <Icon className="h-4 w-4" />
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="p-3">
        <NavLink
          to="/help"
          className="flex items-center gap-2 rounded-md px-3 py-2 text-sm text-text-muted hover:text-text-secondary transition-colors"
        >
          <HelpCircle className="h-4 w-4" />
          Aide
        </NavLink>
      </div>
    </aside>
  );
}
