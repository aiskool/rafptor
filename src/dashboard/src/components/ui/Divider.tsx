import * as Separator from "@radix-ui/react-separator";
import { cn } from "@/lib/utils";

export function Divider({
  orientation = "horizontal",
  className,
}: {
  orientation?: "horizontal" | "vertical";
  className?: string;
}) {
  return (
    <Separator.Root
      orientation={orientation}
      decorative
      className={cn(
        "bg-border",
        orientation === "horizontal" ? "h-px w-full" : "h-full w-px",
        className
      )}
    />
  );
}
