import type { CapacitorConfig } from '@capacitor/cli'
import ptBR from './src/i18n/locales/pt-BR.json'

export default {
  appId: 'br.com.financas.pessoais',
  appName: ptBR.app.nome,
  webDir: 'dist-android',
  loggingBehavior: 'none',
} satisfies CapacitorConfig
