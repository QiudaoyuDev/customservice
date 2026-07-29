/** Tailwind 配置：映射 UI 方案设计系统（色彩 / 字体 / 圆角）。 */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: { DEFAULT: '#0F4C5C', soft: '#E6F0F2' },
        ai: '#1FB6A6',
        human: '#E8833A',
        ink: '#0F172A',
        ink2: '#475569',
        line: '#E2E8F0',
        ok: '#16A34A',
        warn: '#D97706',
        danger: '#DC2626',
        info: '#2563EB',
      },
      fontFamily: {
        sans: [
          'Inter',
          'system-ui',
          '-apple-system',
          'Segoe UI',
          'PingFang SC',
          'Microsoft YaHei',
          'sans-serif',
        ],
        mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
      borderRadius: { xl: '12px', '2xl': '16px' },
      boxShadow: {
        card: '0 1px 2px rgba(15,23,42,.06), 0 8px 24px rgba(15,23,42,.06)',
      },
    },
  },
  plugins: [],
};
