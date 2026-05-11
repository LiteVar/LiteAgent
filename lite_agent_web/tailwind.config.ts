import type { Config } from "tailwindcss";
import plugin from "tailwindcss/plugin";

const config: Config = {
  content: [
    "./src/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        // V3.0.0 Design Tokens
        blue: {
          1: '#ECF6FD',
          6: '#40A5EE',
          10: '#1D4A6B',
        },
        grey: {
          5: '#94A0AB',
          6: '#7C8B98',
          8: '#58636C',
          10: '#383F44',
        },
        red: {
          6: '#CC2D3A',
        },
      },
      backgroundImage: {
        "gradient-radial": "radial-gradient(var(--tw-gradient-stops))",
        "gradient-conic":
          "conic-gradient(from 180deg at 50% 50%, var(--tw-gradient-stops))",
      },
      borderRadius: {
        '3xl': '32px',
      },
    },
  },
  corePlugins: {
    preflight: false, // 禁用 Tailwind 的 base styles，避免与 antd 冲突
  },
  important: true,
  plugins: [
    plugin(function ({ matchVariant }) {
      // 定义一个新的变体
      matchVariant('rp', (value) => {
        return `& ${value.replace(/_/g, ' ')}`;
      });
    }),
  ],
};
export default config;