import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: "class",
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        "on-secondary-fixed-variant": "#404758",
        "error": "#ba1a1a",
        "success-green": "#00703C",
        "border-subtle": "#E5E7EB",
        "tertiary-fixed-dim": "#ffb695",
        "surface-container-lowest": "#ffffff",
        "background": "#f9f9f9",
        "surface-bright": "#f9f9f9",
        "on-tertiary-fixed": "#351000",
        "on-surface-variant": "#464555",
        "surface-dim": "#dadada",
        "on-primary-fixed-variant": "#3323cc",
        "tertiary-fixed": "#ffdbcc",
        "on-background": "#1a1c1c",
        "primary": "#3525cd",
        "secondary-container": "#d9dff5",
        "secondary": "#575e70",
        "on-error-container": "#93000a",
        "surface-container-high": "#e8e8e8",
        "inverse-surface": "#2f3131",
        "text-muted": "#6B7280",
        "on-error": "#ffffff",
        "on-surface": "#1a1c1c",
        "tertiary": "#7e3000",
        "secondary-fixed": "#dce2f7",
        "surface-variant": "#e2e2e2",
        "on-secondary": "#ffffff",
        "on-primary-fixed": "#0f0069",
        "surface-primary": "#FFFFFF",
        "inverse-primary": "#c3c0ff",
        "outline-variant": "#c7c4d8",
        "on-primary": "#ffffff",
        "outline": "#777587",
        "error-container": "#ffdad6",
        "surface-container-highest": "#e2e2e2",
        "surface": "#f9f9f9",
        "tertiary-container": "#a44100",
        "primary-fixed-dim": "#c3c0ff",
        "on-tertiary-fixed-variant": "#7b2f00",
        "primary-container": "#4f46e5",
        "on-secondary-fixed": "#141b2b",
        "primary-fixed": "#e2dfff",
        "on-primary-container": "#dad7ff",
        "surface-container": "#eeeeee",
        "link-blue": "#1D70B8",
        "on-secondary-container": "#5c6274",
        "inverse-on-surface": "#f0f1f1",
        "on-tertiary": "#ffffff",
        "surface-tint": "#4d44e3",
        "secondary-fixed-dim": "#c0c6db",
        "on-tertiary-container": "#ffd2be",
        "surface-container-low": "#f3f3f3"
      },
      borderRadius: {
        "DEFAULT": "0.25rem",
        "lg": "0.5rem",
        "xl": "0.75rem",
        "full": "9999px"
      },
      spacing: {
        "stack-lg": "32px",
        "container-max": "1200px",
        "gutter": "24px",
        "stack-xl": "64px",
        "stack-sm": "8px",
        "base": "8px",
        "stack-md": "16px",
        "margin-mobile": "16px"
      },
      fontFamily: {
        "headline-md": ["var(--font-jakarta)", "sans-serif"],
        "label-xs": ["var(--font-inter)", "sans-serif"],
        "display-lg": ["var(--font-jakarta)", "sans-serif"],
        "body-md": ["var(--font-inter)", "sans-serif"],
        "label-sm": ["var(--font-inter)", "sans-serif"],
        "display-lg-mobile": ["var(--font-jakarta)", "sans-serif"],
        "body-lg": ["var(--font-inter)", "sans-serif"],
        sans: ["var(--font-inter)", "sans-serif"]
      },
      fontSize: {
        "headline-md": ["24px", { lineHeight: "1.3", letterSpacing: "-0.01em", fontWeight: "600" }],
        "label-xs": ["12px", { lineHeight: "1.2", letterSpacing: "0.05em", fontWeight: "600" }],
        "display-lg": ["48px", { lineHeight: "1.1", letterSpacing: "-0.02em", fontWeight: "700" }],
        "body-md": ["16px", { lineHeight: "1.5", fontWeight: "400" }],
        "label-sm": ["14px", { lineHeight: "1.4", letterSpacing: "0.01em", fontWeight: "500" }],
        "display-lg-mobile": ["32px", { lineHeight: "1.2", letterSpacing: "-0.02em", fontWeight: "700" }],
        "body-lg": ["18px", { lineHeight: "1.6", fontWeight: "400" }]
      }
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    require('@tailwindcss/container-queries')
  ],
};
export default config;
