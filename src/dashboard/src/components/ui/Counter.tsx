import { useEffect, useRef, useState } from "react";
import { formatNumber } from "@/lib/utils";

export interface CounterProps {
  value: number;
  duration?: number;
  locale?: string;
  className?: string;
  suffix?: string;
  prefix?: string;
}

const easeOut = (t: number) => 1 - Math.pow(1 - t, 3);

export function Counter({
  value,
  duration = 1500,
  locale = "fr-FR",
  className,
  suffix = "",
  prefix = "",
}: CounterProps) {
  const [display, setDisplay] = useState(0);
  const fromRef = useRef(0);
  const rafRef = useRef<number>();

  useEffect(() => {
    const start = performance.now();
    const from = fromRef.current;
    const delta = value - from;
    const tick = (now: number) => {
      const t = Math.min(1, (now - start) / duration);
      setDisplay(from + delta * easeOut(t));
      if (t < 1) rafRef.current = requestAnimationFrame(tick);
      else fromRef.current = value;
    };
    rafRef.current = requestAnimationFrame(tick);
    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current);
    };
  }, [value, duration]);

  return (
    <span className={className}>
      {prefix}
      {formatNumber(Math.round(display), locale)}
      {suffix}
    </span>
  );
}
