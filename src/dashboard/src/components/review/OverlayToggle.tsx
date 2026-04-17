import * as Slider from "@radix-ui/react-slider";

export interface OverlayToggleProps {
  opacity: number;
  onChange: (v: number) => void;
  className?: string;
}

export function OverlayToggle({ opacity, onChange, className }: OverlayToggleProps) {
  return (
    <div className={className}>
      <label className="mb-1 flex items-center justify-between text-xs text-text-secondary">
        <span>Superposition</span>
        <span className="tabular-nums">{Math.round(opacity * 100)}%</span>
      </label>
      <Slider.Root
        className="relative flex h-5 w-full touch-none select-none items-center"
        min={0}
        max={1}
        step={0.01}
        value={[opacity]}
        onValueChange={(v) => onChange(v[0] ?? 0)}
      >
        <Slider.Track className="relative h-1 w-full grow rounded-full bg-bg-overlay">
          <Slider.Range className="absolute h-full rounded-full bg-accent" />
        </Slider.Track>
        <Slider.Thumb
          className="block h-3.5 w-3.5 rounded-full border-2 border-accent bg-white focus:outline-none focus:ring-2 focus:ring-accent-ring"
          aria-label="Opacité"
        />
      </Slider.Root>
    </div>
  );
}
