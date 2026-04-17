import { motion } from "framer-motion";
import { cn } from "@/lib/utils";

export interface StepIndicatorProps {
  current: number;
  total: number;
  label?: string;
  className?: string;
}

export function StepIndicator({ current, total, label, className }: StepIndicatorProps) {
  return (
    <div className={cn("flex flex-col items-center gap-2 text-text-muted", className)}>
      <div className="flex items-center gap-1.5">
        {Array.from({ length: total }).map((_, i) => (
          <motion.span
            key={i}
            className={cn(
              "h-1.5 rounded-full",
              i < current ? "bg-accent" : i === current ? "bg-accent/80" : "bg-bg-overlay"
            )}
            animate={{ width: i === current ? 28 : 8 }}
            transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
          />
        ))}
      </div>
      {label && <span className="text-xs">{label}</span>}
    </div>
  );
}
