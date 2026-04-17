import { NavLink } from "react-router-dom";
import { BarChart3, FileText, Eye, GitBranch, Type, ClipboardList } from "lucide-react";
import clsx from "clsx";

const NAV = [
  { to: "/", label: "Dashboard", icon: BarChart3 },
  { to: "/pipelines", label: "Pipelines", icon: GitBranch },
  { to: "/documents", label: "Documents", icon: FileText },
  { to: "/review", label: "Review", icon: Eye },
  { to: "/fonts", label: "Fonts", icon: Type },
  { to: "/audit", label: "Audit", icon: ClipboardList },
];

export function Sidebar() {
  return (
    <aside className="w-56 border-r border-white/5 bg-bg-secondary p-4 h-screen">
      <div className="mb-8 font-mono text-lg font-semibold text-text-primary">RAFPTOR</div>
      <nav className="space-y-1">
        {NAV.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === "/"}
            className={({ isActive }) =>
              clsx(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm",
                isActive
                  ? "bg-accent/20 text-accent"
                  : "text-text-secondary hover:bg-bg-tertiary hover:text-text-primary",
              )
            }
          >
            <Icon size={16} />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
