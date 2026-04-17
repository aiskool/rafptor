import { useMemo } from "react";
import { motion } from "framer-motion";

const COLORS = ["#6366f1", "#818cf8", "#22c55e", "#eab308", "#fafafa"];

export function Confetti({ pieces = 80 }: { pieces?: number }) {
  const particles = useMemo(
    () =>
      Array.from({ length: pieces }).map((_, i) => ({
        id: i,
        left: Math.random() * 100,
        size: 4 + Math.random() * 6,
        color: COLORS[i % COLORS.length],
        delay: Math.random() * 0.6,
        duration: 2.4 + Math.random() * 1.6,
        rotate: Math.random() * 360,
      })),
    [pieces]
  );

  return (
    <div className="pointer-events-none fixed inset-0 z-[200] overflow-hidden">
      {particles.map((p) => (
        <motion.span
          key={p.id}
          className="absolute top-[-5%] block rounded-[2px]"
          style={{ left: `${p.left}%`, width: p.size, height: p.size * 2, background: p.color }}
          initial={{ y: -40, rotate: 0, opacity: 1 }}
          animate={{ y: "110vh", rotate: p.rotate + 720, opacity: [1, 1, 0] }}
          transition={{ duration: p.duration, delay: p.delay, ease: [0.4, 0, 0.5, 1] }}
        />
      ))}
    </div>
  );
}
