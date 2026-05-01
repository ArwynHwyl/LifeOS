/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,ts}",    // ← tells tailwind to scan your files
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}