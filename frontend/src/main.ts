import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { inicializarTema } from './theme'
import { i18n, inicializarIdioma } from './i18n'

inicializarTema()
inicializarIdioma()

const app = createApp(App)

app.use(router)
app.use(i18n)

app.mount('#app')
