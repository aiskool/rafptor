import { NavLink } from "react-router-dom";
import { CheckCircle2, FileText, LayoutDashboard, Settings } from "lucide-react";
import { cn } from "@/lib/utils";

const NAV = [
  { to: "/dashboard", label: "Vue", icon: LayoutDashboard },
  { to: "/documents", label: "Documents", icon: FileText },
  { to: "/review", label: "Vérifier", icon: CheckCircle2 },
  { to: "/settings", label: "Paramètres", icon: Settings },
];

export function MobileNav() {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 flex h-14 border-t border-border bg-bg-raised/90 backdrop-blur md:hidden">
      {NAV.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          className={({ isActive }) =>
            cn(
              "flex flex-1 flex-col items-center justify-center gap-0.5 text-[11px] transition-colors",
              isActive ? "text-accent" : "text-text-secondary"
            )
          }
        >
          <Icon className="h-5 w-5" />
          {label}
        </NavLink>
      ))}
    </nav>
  );
}
