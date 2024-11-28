import type { Config } from "tailwindcss";
import plugin from "tailwindcss/plugin";

const config: Config = {
  content: [
    "./src/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      backgroundImage: {
        "gradient-radial": "radial-gradient(var(--tw-gradient-stops))",
        "gradient-conic":
          "conic-gradient(from 180deg at 50% 50%, var(--tw-gradient-stops))",
      },
    },
  },
  corePlugins: {
    preflight: false, // 禁用 Tailwind 的 base styles
  },
  important:true,
  plugins: [
    plugin(function ({matchVariant}) {
      // 定义一个新的变体
      matchVariant('rp', (value) => {
        return `& ${value.replace(/_/g, ' ')}`;
      });
    }),
  ],
};
export default config;
