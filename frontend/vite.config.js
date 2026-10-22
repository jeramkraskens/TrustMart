import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { randomUUID } from 'node:crypto'

export default defineConfig({
  define: {
    'import.meta.env.VITE_APP_RUN_ID': JSON.stringify(randomUUID()),
  },
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    open: '/',
  },
})
