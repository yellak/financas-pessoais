import { Capacitor } from '@capacitor/core'
import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import { sessaoApi } from '@/services/sessao'
import CategoriasView from '../views/CategoriasView.vue'
import LoginView from '../views/LoginView.vue'

const router = createRouter({
  history: Capacitor.isNativePlatform()
    ? createWebHashHistory(import.meta.env.BASE_URL)
    : createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/categorias' },
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/categorias',
      name: 'categorias',
      component: CategoriasView,
      meta: { requerSessao: true },
    },
  ],
})

router.beforeEach(async (destino) => {
  if (!destino.meta.requerSessao) return
  try {
    if (await sessaoApi.atual()) return
  } catch {
    return { name: 'login', query: { sessao: 'indisponivel' } }
  }
  return { name: 'login' }
})

export default router
