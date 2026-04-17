import { motion } from "framer-motion";
import { cn } from "@/lib/utils";

export interface ProgressProps {
  value: number;
  max?: number;
  className?: string;
  size?: "sm" | "md" | "lg";
  indeterminate?: boolean;
}

const heights = { sm: "h-1", md: "h-2", lg: "h-3" } as const;

export function Progress({
  value,
  max = 100,
  className,
  size = "md",
  indeterminate = false,
}: ProgressProps) {
  const pct = Math.min(100, Math.max(0, (value / max) * 100));
  return (
    <div
      role="progressbar"
      aria-valuenow={indeterminate ? undefined : value}
      aria-valuemin={0}
      aria-valuemax={max}
      className={cn("w-full overflow-hidden rounded-full bg-bg-overlay", heights[size], className)}
    >
      {indeterminate ? (
        <motion.div
          className="h-full w-1/3 rounded-full bg-accent"
          animate={{ x: ["-100%", "400%"] }}
          transition={{ repeat: Infinity, duration: 1.6, ease: "easeInOut" }}
        />
      ) : (
        <motion.div
          className="h-full rounded-full bg-accent"
          initial={{ width: 0 }}
          animate={{ width: `${pct}%` }}
          transition={{ duration: 0.6, ease: [0.4, 0, 0.2, 1] }}
        />
      )}
    </div>
  );
}
