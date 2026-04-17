import clsx from "clsx";

interface Props {
  grid: number[][];
  threshold?: number;
}

export function ZoneHeatmap({ grid, threshold = 0.85 }: Props) {
  if (!grid.length) {
    return null;
  }
  return (
    <div className="grid gap-0.5" style={{ gridTemplateColumns: `repeat(${grid[0].length}, minmax(0, 1fr))` }}>
      {grid.flatMap((row, r) =>
        row.map((score, c) => (
          <div
            key={`${r}-${c}`}
            title={`SSIM ${score.toFixed(3)}`}
            className={clsx(
              "h-4 w-full rounded-sm",
              score >= threshold ? "bg-success/20" : "bg-danger/40",
            )}
          />
        )),
      )}
    </div>
  );
}
