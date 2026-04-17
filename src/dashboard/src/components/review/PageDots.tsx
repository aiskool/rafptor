import { motion } from "framer-motion";
import { cn } from "@/lib/utils";

export interface PageDotsProps {
  current: number;
  total: number;
  onChange: (page: number) => void;
  className?: string;
}

export function PageDots({ current, total, onChange, className }: PageDotsProps) {
  return (
    <div className={cn("flex items-center gap-1.5", className)}>
      {Array.from({ length: total }).map((_, i) => {
        const active = i === current;
        return (
          <motion.button
            key={i}
            type="button"
            onClick={() => onChange(i)}
            aria-label={`Aller à la page ${i + 1}`}
            aria-current={active}
            className={cn(
              "h-2 rounded-full transition-colors",
              active ? "bg-accent" : "bg-bg-overlay hover:bg-border-hover"
            )}
            animate={{ width: active ? 20 : 8 }}
            transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
          />
        );
      })}
    </div>
  );
}
