/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        ecusol: {
          primario: '#1E3A8A',      // Azul oscuro elegante
          secundario: '#FCD34D',    // Amarillo dorado
          terciario: '#FBBF24',     // Amarillo brillante
          fondo: '#F0F9FF',         // Azul muy claro para fondos
          texto: '#1E293B',         // Gris azulado oscuro
          rojo: '#DC2626',
          verde: '#16A34A',
          'gris-claro': '#F1F5F9',  // Gris azulado suave
          'gris-oscuro': '#475569', // Gris azulado oscuro
          'azul-medio': '#3B82F6',   // Azul medio para enlaces
          'amarillo-claro': '#FEF3C7' // Amarillo suave
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      }
    },
  },
  plugins: [],
}