import { motion } from "framer-motion";
import { cn } from "@/lib/utils";

export interface ProgressRingProps {
  value: number;
  size?: number;
  stroke?: number;
  className?: string;
  trackColor?: string;
  valueColor?: string;
  label?: string;
}

export function ProgressRing({
  value,
  size = 96,
  stroke = 8,
  className,
  trackColor = "rgba(255,255,255,0.08)",
  valueColor = "#6366f1",
  label,
}: ProgressRingProps) {
  const pct = Math.min(100, Math.max(0, value));
  const radius = (size - stroke) / 2;
  const circ = 2 * Math.PI * radius;
  const offset = circ * (1 - pct / 100);

  return (
    <div className={cn("relative inline-flex items-center justify-center", className)}>
      <svg width={size} height={size} className="-rotate-90">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke={trackColor}
          strokeWidth={stroke}
          fill="none"
        />
        <motion.circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke={valueColor}
          strokeWidth={stroke}
          strokeLinecap="round"
          fill="none"
          strokeDasharray={circ}
          initial={{ strokeDashoffset: circ }}
          animate={{ strokeDashoffset: offset }}
          transition={{ duration: 0.8, ease: [0.4, 0, 0.2, 1] }}
        />
      </svg>
      <div className="absolute text-center">
        <div className="text-lg font-semibold text-text-primary tabular-nums">{Math.round(pct)}%</div>
        {label && <div className="text-[10px] uppercase tracking-wider text-text-muted">{label}</div>}
      </div>
    </div>
  );
}
