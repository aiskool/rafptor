import { motion } from "framer-motion";
import { cn } from "@/lib/utils";

export function PulseAnimation({ className, size = 120 }: { className?: string; size?: number }) {
  return (
    <div
      className={cn("relative flex items-center justify-center", className)}
      style={{ width: size, height: size }}
    >
      {[0, 0.6, 1.2].map((delay) => (
        <motion.span
          key={delay}
          className="absolute inset-0 rounded-full border border-accent/40"
          animate={{ scale: [0.6, 1.6], opacity: [0.6, 0] }}
          transition={{
            repeat: Infinity,
            duration: 2.4,
            ease: "easeOut",
            delay,
          }}
        />
      ))}
      <span className="relative h-4 w-4 rounded-full bg-accent shadow-[0_0_24px_rgba(99,102,241,0.6)]" />
    </div>
  );
}
