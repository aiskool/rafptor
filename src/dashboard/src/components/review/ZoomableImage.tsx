import { useCallback, useRef, useState, type WheelEvent } from "react";
import { cn } from "@/lib/utils";

export interface ZoomableImageProps {
  src: string;
  alt?: string;
  className?: string;
  zoom: number;
  onZoomChange: (zoom: number) => void;
}

export function ZoomableImage({ src, alt = "", className, zoom, onZoomChange }: ZoomableImageProps) {
  const [translate, setTranslate] = useState({ x: 0, y: 0 });
  const dragging = useRef<{ x: number; y: number } | null>(null);

  const onWheel = (e: WheelEvent<HTMLDivElement>) => {
    if (!e.ctrlKey && !e.metaKey) return;
    e.preventDefault();
    const delta = -e.deltaY * 0.002;
    onZoomChange(Math.min(4, Math.max(0.5, zoom + delta)));
  };

  const onDoubleClick = () => {
    onZoomChange(zoom === 1 ? 2 : 1);
    setTranslate({ x: 0, y: 0 });
  };

  const onMouseDown = (e: React.MouseEvent) => {
    if (zoom <= 1) return;
    dragging.current = { x: e.clientX - translate.x, y: e.clientY - translate.y };
  };

  const onMouseMove = useCallback(
    (e: React.MouseEvent) => {
      if (!dragging.current) return;
      setTranslate({ x: e.clientX - dragging.current.x, y: e.clientY - dragging.current.y });
    },
    []
  );

  const onMouseUp = () => {
    dragging.current = null;
  };

  return (
    <div
      onWheel={onWheel}
      onDoubleClick={onDoubleClick}
      onMouseDown={onMouseDown}
      onMouseMove={onMouseMove}
      onMouseUp={onMouseUp}
      onMouseLeave={onMouseUp}
      className={cn(
        "relative h-full w-full overflow-hidden bg-bg-elevated",
        zoom > 1 ? "cursor-grab active:cursor-grabbing" : "cursor-zoom-in",
        className
      )}
    >
      <img
        src={src}
        alt={alt}
        draggable={false}
        className="h-full w-full select-none object-contain transition-transform duration-100 ease-out"
        style={{
          transform: `translate(${translate.x}px, ${translate.y}px) scale(${zoom})`,
          transformOrigin: "center center",
        }}
      />
    </div>
  );
}
