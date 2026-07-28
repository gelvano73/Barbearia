/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: '#12100e',
        'bg-elevated': '#1c1814',
        'bg-panel': '#241f19',
        ink: '#f3ece3',
        muted: '#a89a88',
        brass: '#c9a227',
        'brass-deep': '#9a7b1a',
        danger: '#c45c4a',
        ok: '#6f9e6b',
      },
      fontFamily: {
        display: ['"Bebas Neue"', 'sans-serif'],
        body: ['"DM Sans"', 'sans-serif'],
      },
      borderRadius: {
        panel: '14px',
      },
      boxShadow: {
        panel: '0 24px 60px rgba(0, 0, 0, 0.45)',
      },
    },
  },
  plugins: [],
}
