import { useCallback, useRef, useState, type ReactNode } from "react";
import { cn } from "@/lib/utils";

export interface SplitViewProps {
  left: ReactNode;
  right: ReactNode;
  leftLabel?: string;
  rightLabel?: string;
  className?: string;
  initialSplit?: number;
}

export function SplitView({
  left,
  right,
  leftLabel,
  rightLabel,
  className,
  initialSplit = 50,
}: SplitViewProps) {
  const [split, setSplit] = useState(initialSplit);
  const dragging = useRef(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const onMove = useCallback((e: MouseEvent) => {
    if (!dragging.current || !containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const pct = ((e.clientX - rect.left) / rect.width) * 100;
    setSplit(Math.min(80, Math.max(20, pct)));
  }, []);

  const onUp = useCallback(() => {
    dragging.current = false;
    document.body.style.cursor = "";
    document.removeEventListener("mousemove", onMove);
    document.removeEventListener("mouseup", onUp);
  }, [onMove]);

  const onDown = () => {
    dragging.current = true;
    document.body.style.cursor = "col-resize";
    document.addEventListener("mousemove", onMove);
    document.addEventListener("mouseup", onUp);
  };

  return (
    <div
      ref={containerRef}
      className={cn("relative flex h-full overflow-hidden rounded-lg border border-border bg-bg-raised", className)}
    >
      <div className="relative flex h-full flex-col overflow-hidden" style={{ width: `${split}%` }}>
        {leftLabel && (
          <div className="absolute left-3 top-3 z-10 rounded-full border border-border bg-bg-overlay/80 px-2.5 py-0.5 text-xs text-text-secondary backdrop-blur">
            {leftLabel}
          </div>
        )}
        <div className="flex-1 overflow-auto">{left}</div>
      </div>
      <button
        type="button"
        onMouseDown={onDown}
        className="group relative w-1 cursor-col-resize bg-border transition-colors hover:bg-accent"
        aria-label="Redimensionner les panneaux"
      >
        <span className="absolute left-1/2 top-1/2 h-8 w-8 -translate-x-1/2 -translate-y-1/2 rounded-full bg-accent/0 group-hover:bg-accent/10 transition-colors" />
      </button>
      <div className="relative flex h-full flex-col overflow-hidden" style={{ width: `${100 - split}%` }}>
        {rightLabel && (
          <div className="absolute right-3 top-3 z-10 rounded-full border border-border bg-bg-overlay/80 px-2.5 py-0.5 text-xs text-text-secondary backdrop-blur">
            {rightLabel}
          </div>
        )}
        <div className="flex-1 overflow-auto">{right}</div>
      </div>
    </div>
  );
}
