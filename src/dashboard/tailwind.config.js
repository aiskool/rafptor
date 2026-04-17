/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        bg: {
          primary: "#0a0b0d",
          secondary: "#111216",
          tertiary: "#1a1b21",
          card: "#16171c",
        },
        text: {
          primary: "#e8e6e1",
          secondary: "#9d9b95",
          muted: "#5e5d58",
        },
        accent: "#6e8efb",
        success: "#3ecf8e",
        warning: "#f5a623",
        danger: "#e85d5d",
        info: "#2dd4bf",
        score: {
          high: "#3ecf8e",
          medium: "#f5a623",
          low: "#e85d5d",
        },
      },
      fontFamily: {
        sans: ["DM Sans", "system-ui", "sans-serif"],
        mono: ["DM Mono", "Fira Code", "monospace"],
      },
    },
  },
  plugins: [],
};
