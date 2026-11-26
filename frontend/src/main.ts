import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { inicializarTema } from './theme'
import { i18n, inicializarIdioma } from './i18n'
import { registrarErro } from './logger'

inicializarTema()
inicializarIdioma()

const app = createApp(App)

app.config.errorHandler = (falha, _instancia, origem) => {
  const { matched } = router.currentRoute.value
  registrarErro({
    evento: 'erro_vue',
    origem,
    rota: matched[matched.length - 1]?.path,
    tipoErro: falha instanceof Error ? falha.constructor.name : typeof falha,
  })
}

app.use(router)
app.use(i18n)

app.mount('#app')
