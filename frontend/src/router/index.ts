import { Capacitor } from '@capacitor/core'
import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import CategoriasView from '../views/CategoriasView.vue'

const router = createRouter({
  history: Capacitor.isNativePlatform()
    ? createWebHashHistory(import.meta.env.BASE_URL)
    : createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/categorias' },
    { path: '/categorias', component: CategoriasView },
  ],
})

export default router
