import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import App from '../App.vue'
import CategoriasView from '../views/CategoriasView.vue'

afterEach(() => vi.unstubAllGlobals())

describe('App', () => {
  it('abre categorias a partir da página inicial', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('[]')))
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', redirect: '/categorias' },
        { path: '/categorias', component: CategoriasView },
      ],
    })
    await router.push('/')
    await router.isReady()
    const wrapper = mount(App, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.get('h1').text()).toBe('Categorias')
    expect(wrapper.get('nav a').attributes('href')).toBe('/categorias')
    wrapper.unmount()
  })
})
