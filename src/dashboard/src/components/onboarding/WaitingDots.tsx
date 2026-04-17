import { motion } from "framer-motion";

export function WaitingDots({ className }: { className?: string }) {
  return (
    <span className={className} aria-label="En cours">
      {[0, 0.15, 0.3].map((delay) => (
        <motion.span
          key={delay}
          className="mx-0.5 inline-block h-1 w-1 rounded-full bg-text-muted"
          animate={{ opacity: [0.2, 1, 0.2] }}
          transition={{ repeat: Infinity, duration: 1.2, delay, ease: "easeInOut" }}
        />
      ))}
    </span>
  );
}
