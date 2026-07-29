/** Tailwind 配置：映射 UI 方案设计系统（色彩 / 字体 / 圆角 / 阴影 / 动效）。 */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        canvas: '#F5F8F9',
        brand: {
          DEFAULT: '#0F4C5C',
          50: '#EAF2F4',
          100: '#D6E7EC',
          500: '#1B7E97',
          600: '#14647A',
          700: '#0F4C5C',
          800: '#0B3F4C',
          900: '#07323C',
          soft: '#E6F0F2',
        },
        ai: {
          DEFAULT: '#1FB6A6',
          100: '#D7F5F1',
          500: '#1FB6A6',
          600: '#0E9D8F',
          soft: '#E6F9F6',
        },
        human: {
          DEFAULT: '#E8833A',
          100: '#FDEBDD',
          500: '#E8833A',
          600: '#C96A1F',
          soft: '#FFF3E9',
        },
        safety: { DEFAULT: '#E11D48', bg: '#FFE4E6' },
        ink: {
          DEFAULT: '#0F172A',
          2: '#475569',
          3: '#94A3B8',
        },
        line: '#E2E8F0',
        ok: { DEFAULT: '#16A34A', bg: '#DCFCE7' },
        warn: { DEFAULT: '#D97706', bg: '#FEF3C7' },
        danger: { DEFAULT: '#DC2626', bg: '#FEE2E2' },
        info: { DEFAULT: '#2563EB', bg: '#DBEAFE' },
      },
      fontFamily: {
        sans: [
          'Manrope',
          'system-ui',
          '-apple-system',
          'Segoe UI',
          'PingFang SC',
          'Microsoft YaHei',
          'sans-serif',
        ],
        display: ['Sora', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
      borderRadius: { xl: '12px', '2xl': '16px', '3xl': '20px' },
      boxShadow: {
        xs: '0 1px 2px rgba(15,23,42,.04)',
        card: '0 1px 2px rgba(15,23,42,.06), 0 8px 24px rgba(15,23,42,.06)',
        pop: '0 12px 32px rgba(15,23,42,.12)',
        inset: 'inset 0 1px 0 rgba(255,255,255,.6)',
      },
      keyframes: {
        'fade-in': { from: { opacity: '0' }, to: { opacity: '1' } },
        'slide-up': {
          from: { opacity: '0', transform: 'translateY(10px)' },
          to: { opacity: '1', transform: 'none' },
        },
        'pop-in': {
          from: { opacity: '0', transform: 'scale(.97)' },
          to: { opacity: '1', transform: 'none' },
        },
        'pulse-ring': {
          '0%': { boxShadow: '0 0 0 0 rgba(31,182,166,.5)' },
          '70%': { boxShadow: '0 0 0 8px rgba(31,182,166,0)' },
          '100%': { boxShadow: '0 0 0 0 rgba(31,182,166,0)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        spin: { to: { transform: 'rotate(360deg)' } },
      },
      animation: {
        'fade-in': 'fade-in .3s ease',
        'slide-up': 'slide-up .4s cubic-bezier(.2,.7,.3,1)',
        'pop-in': 'pop-in .2s ease',
        'pulse-ring': 'pulse-ring 1.8s ease-out infinite',
        shimmer: 'shimmer 1.4s linear infinite',
        spin: 'spin 1s linear infinite',
      },
    },
  },
  plugins: [],
};
