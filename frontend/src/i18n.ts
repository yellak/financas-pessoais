import { watchEffect } from 'vue'
import { createI18n } from 'vue-i18n'
import ptBR from './i18n/locales/pt-BR.json'

export const idiomaPadrao = 'pt-BR'

export const i18n = createI18n({
  legacy: false,
  locale: idiomaPadrao,
  fallbackLocale: idiomaPadrao,
  messages: { 'pt-BR': ptBR },
})

export function inicializarIdioma(chaveTitulo = () => 'app.titulo') {
  watchEffect(() => {
    document.documentElement.lang = i18n.global.locale.value
    document.title = i18n.global.t(chaveTitulo())
    document
      .querySelector('meta[name="description"]')
      ?.setAttribute('content', i18n.global.t('app.descricao'))
  })
}
