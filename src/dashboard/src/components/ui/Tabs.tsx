import * as RadixTabs from "@radix-ui/react-tabs";
import { cn } from "@/lib/utils";

export const Tabs = RadixTabs.Root;

export function TabsList({ className, ...props }: RadixTabs.TabsListProps) {
  return (
    <RadixTabs.List
      className={cn(
        "inline-flex items-center gap-1 rounded-full border border-border bg-bg-raised p-1",
        className
      )}
      {...props}
    />
  );
}

export function TabsTrigger({ className, ...props }: RadixTabs.TabsTriggerProps) {
  return (
    <RadixTabs.Trigger
      className={cn(
        "inline-flex items-center justify-center gap-1.5 rounded-full px-4 py-1.5 text-sm font-medium",
        "text-text-secondary transition-colors duration-base",
        "hover:text-text-primary",
        "data-[state=active]:bg-accent data-[state=active]:text-white",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent-ring",
        className
      )}
      {...props}
    />
  );
}

export const TabsContent = RadixTabs.Content;
