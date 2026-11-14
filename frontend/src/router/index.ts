import { createRouter, createWebHistory } from 'vue-router'
import CategoriasView from '../views/CategoriasView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/categorias' },
    { path: '/categorias', component: CategoriasView },
  ],
})

export default router
