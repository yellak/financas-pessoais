import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { aplicarTema, temaPadrao } from './theme'

aplicarTema(temaPadrao)

const app = createApp(App)

app.use(router)

app.mount('#app')
