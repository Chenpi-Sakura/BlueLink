/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'parchment-50': '#FDFBF7',
        'parchment-100': '#F4EFE6', 
        'parchment-200': '#E8DFD0',
        'ink-900': '#2C2B29',
        'ink-600': '#66635D',
        'ink-400': '#A39F98',
        'brand-accent': '#002FA7',
        'brand-accent-light': '#6B8DFF',
        'brand-accent-bg': 'rgba(0, 47, 167, 0.08)',
      },
      fontFamily: {
        'sans': ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', 'sans-serif'],
        'serif': ['"Noto Serif SC"', 'ui-serif', 'Georgia', 'serif'],
      },
      borderRadius: {
        '2xl': '1.25rem',
        '3xl': '1.75rem',
        '4xl': '2.5rem',
      },
      boxShadow: {
        'soft': '0 8px 30px -4px rgba(44, 43, 41, 0.04)',
        'float': '0 20px 40px -10px rgba(0, 47, 167, 0.15)',
        'card': '0 4px 20px -2px rgba(44, 43, 41, 0.06)',
      }
    },
  },
  plugins: [],
}
