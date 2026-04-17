import animate from "tailwindcss-animate";

/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        bg: {
          base: "#09090b",
          raised: "#111113",
          elevated: "#18181b",
          overlay: "#27272a",
        },
        text: {
          primary: "#fafafa",
          secondary: "#a1a1aa",
          muted: "#52525b",
          inverse: "#09090b",
        },
        accent: {
          DEFAULT: "#6366f1",
          hover: "#818cf8",
          subtle: "rgba(99,102,241,0.12)",
          ring: "rgba(99,102,241,0.5)",
        },
        success: {
          DEFAULT: "#22c55e",
          subtle: "rgba(34,197,94,0.12)",
        },
        warning: {
          DEFAULT: "#eab308",
          subtle: "rgba(234,179,8,0.12)",
        },
        danger: {
          DEFAULT: "#ef4444",
          subtle: "rgba(239,68,68,0.12)",
        },
        border: {
          DEFAULT: "rgba(255,255,255,0.06)",
          hover: "rgba(255,255,255,0.12)",
          focus: "rgba(99,102,241,0.5)",
        },
      },
      fontFamily: {
        sans: ['"Inter Variable"', "Inter", "system-ui", "sans-serif"],
        mono: ['"JetBrains Mono"', '"Fira Code"', "monospace"],
      },
      borderRadius: {
        sm: "6px",
        md: "10px",
        lg: "14px",
        xl: "20px",
      },
      transitionTimingFunction: {
        smooth: "cubic-bezier(0.4, 0, 0.2, 1)",
      },
      transitionDuration: {
        fast: "150ms",
        base: "250ms",
        slow: "400ms",
      },
      keyframes: {
        pageEnter: {
          from: { opacity: "0", transform: "translateY(8px)" },
          to: { opacity: "1", transform: "translateY(0)" },
        },
        shimmer: {
          "0%": { backgroundPosition: "-200% 0" },
          "100%": { backgroundPosition: "200% 0" },
        },
        pulseRing: {
          "0%": { transform: "scale(0.8)", opacity: "0.6" },
          "100%": { transform: "scale(2.2)", opacity: "0" },
        },
      },
      animation: {
        pageEnter: "pageEnter 0.4s cubic-bezier(0.4, 0, 0.2, 1)",
        shimmer: "shimmer 1.6s linear infinite",
        pulseRing: "pulseRing 1.8s ease-out infinite",
      },
      boxShadow: {
        glow: "0 0 0 1px rgba(255,255,255,0.04), 0 20px 40px rgba(0,0,0,0.5)",
        ring: "0 0 0 3px rgba(99,102,241,0.3)",
      },
    },
  },
  plugins: [animate],
};
