import { forwardRef, useId, type InputHTMLAttributes, type ReactNode } from "react";
import { cn } from "@/lib/utils";

export interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "prefix"> {
  label?: string;
  error?: string;
  prefix?: ReactNode;
  suffix?: ReactNode;
  hint?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, prefix, suffix, hint, id, ...props }, ref) => {
    const uid = useId();
    const inputId = id ?? uid;
    return (
      <div className="flex flex-col gap-1.5">
        {label && (
          <label htmlFor={inputId} className="text-sm font-medium text-text-secondary">
            {label}
          </label>
        )}
        <div
          className={cn(
            "flex items-center gap-2 rounded-md border border-border bg-bg-elevated px-3",
            "transition-colors duration-base focus-within:border-border-focus focus-within:ring-2 focus-within:ring-accent-ring",
            error && "border-danger focus-within:border-danger focus-within:ring-danger/30",
            className
          )}
        >
          {prefix && <span className="text-text-muted">{prefix}</span>}
          <input
            id={inputId}
            ref={ref}
            className="w-full bg-transparent py-2.5 text-sm text-text-primary placeholder:text-text-muted focus:outline-none"
            {...props}
          />
          {suffix && <span className="text-text-muted">{suffix}</span>}
        </div>
        {hint && !error && <p className="text-xs text-text-muted">{hint}</p>}
        {error && <p className="text-xs text-danger">{error}</p>}
      </div>
    );
  }
);
Input.displayName = "Input";
