import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import ptBR from './src/i18n/locales/pt-BR.json' with { type: 'json' }

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    {
      name: 'endereco-api-android',
      apply: 'build',
      config(_, { mode }) {
        if (mode === 'android') return { build: { outDir: 'dist-android' } }
      },
      configResolved({ mode, env }) {
        if (mode !== 'android') return

        const endereco = env.VITE_API_URL?.trim()
        const url = endereco && URL.canParse(endereco) ? new URL(endereco) : null
        if (
          !url ||
          !['http:', 'https:'].includes(url.protocol) ||
          url.username ||
          url.password ||
          url.search ||
          url.hash
        ) {
          throw new Error('Defina VITE_API_URL em .env.android.local com o endereço HTTP(S) da API.')
        }
      },
    },
    {
      name: 'metadados-traduzidos',
      transformIndexHtml: (html) => ({
        html: html.replace('%APP_TITLE%', () =>
          ptBR.app.titulo.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;'),
        ),
        tags: [
          {
            tag: 'meta',
            attrs: { name: 'description', content: ptBR.app.descricao },
            injectTo: 'head',
          },
        ],
      }),
    },
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
