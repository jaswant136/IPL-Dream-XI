/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#101114',
        panel: '#17181C',
        edge: '#292A30',
        gold: { DEFAULT: '#F5C84B', dim: '#c9a23a' },
        snow: '#F3F3F4',
        mute: '#92929A',
        good: '#4ADE80',
        bad: '#FB7185',
      },
      fontFamily: {
        display: ['"Barlow Condensed"', 'Impact', 'sans-serif'],
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: { card: '0 10px 30px -12px rgba(0,0,0,.7)' },
    },
  },
  plugins: [],
}
