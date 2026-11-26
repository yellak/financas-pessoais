import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import App from '../App.vue'
import CategoriasView from '../views/CategoriasView.vue'
import { inicializarTema } from '../theme'
import { i18n } from '@/i18n'

let wrapper: VueWrapper | undefined

async function montar() {
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
  wrapper = mount(App, { attachTo: document.body, global: { plugins: [router, i18n] } })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  localStorage.clear()
  inicializarTema()
})

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  document.body.innerHTML = ''
  localStorage.clear()
  delete document.documentElement.dataset.tema
  vi.unstubAllGlobals()
})

describe('App', () => {
  it('abre categorias a partir da página inicial', async () => {
    const pagina = await montar()

    expect(pagina.get('h1').text()).toBe('Categorias')
    expect(pagina.find('nav').exists()).toBe(false)
    const botao = pagina.get('[aria-label="Menu principal"]')
    expect(botao.attributes('aria-expanded')).toBe('false')

    await botao.trigger('click')
    expect(botao.attributes('aria-expanded')).toBe('true')
    expect(pagina.get('nav').attributes('id')).toBe(botao.attributes('aria-controls'))
    expect(pagina.get('nav a').attributes('href')).toBe('/categorias')
    expect(pagina.get('nav a').text()).toBe('Categorias')
    await pagina.get('nav a').trigger('click')
    await flushPromises()

    expect(pagina.find('nav').exists()).toBe(false)
    expect(botao.element).toBe(document.activeElement)
    expect(pagina.get('h1').text()).toBe('Categorias')
  })

  it('permite fechar o menu pelo próprio botão', async () => {
    const pagina = await montar()
    const botao = pagina.get('[aria-label="Menu principal"]')

    await botao.trigger('click')
    await botao.trigger('click')

    expect(pagina.find('nav').exists()).toBe(false)
    expect(botao.attributes('aria-expanded')).toBe('false')
  })

  it('fecha com Esc e devolve o foco ao botão', async () => {
    const pagina = await montar()
    const botao = pagina.get('[aria-label="Menu principal"]')
    await botao.trigger('click')
    expect(pagina.get('nav a').element).toBe(document.activeElement)

    await pagina.get('nav a').trigger('keydown', { key: 'Escape' })

    expect(pagina.find('nav').exists()).toBe(false)
    expect(botao.element).toBe(document.activeElement)
  })

  it('fecha ao clicar fora do menu', async () => {
    const pagina = await montar()
    await pagina.get('[aria-label="Menu principal"]').trigger('click')

    document.body.dispatchEvent(new Event('pointerdown', { bubbles: true }))
    await flushPromises()

    expect(pagina.find('nav').exists()).toBe(false)
    expect(pagina.get('[aria-label="Menu principal"]').attributes('aria-expanded')).toBe('false')
  })

  it('fecha quando o teclado leva o foco para fora do menu', async () => {
    const pagina = await montar()
    await pagina.get('[aria-label="Menu principal"]').trigger('click')
    const atualizar = pagina.get('.list-panel .panel-heading button')

    ;(atualizar.element as HTMLButtonElement).focus()
    await flushPromises()

    expect(pagina.find('nav').exists()).toBe(false)
    expect(atualizar.element).toBe(document.activeElement)
  })

  it('lista Categorias antes da opção Tema e expande as escolhas', async () => {
    const pagina = await montar()
    await pagina.get('[aria-label="Menu principal"]').trigger('click')

    const opcoes = pagina.findAll('nav > ul > li').map((item) => item.get('a, button').text())
    expect(opcoes).toEqual(['Categorias', 'Tema'])
    expect(pagina.find('fieldset').exists()).toBe(false)
    const tema = pagina.get('nav button')
    await tema.trigger('click')

    expect(tema.attributes('aria-expanded')).toBe('true')
    expect(pagina.get('fieldset').attributes('id')).toBe(tema.attributes('aria-controls'))
    expect(pagina.get('fieldset').attributes('aria-label')).toBe('Tema da aplicação')
    expect(pagina.findAll('fieldset label').map((opcao) => opcao.text())).toEqual([
      'Azul bebê',
      'Verde',
    ])
    expect((pagina.get('input[value="azul-bebe"]').element as HTMLInputElement).checked).toBe(true)
    expect(pagina.get('input[value="azul-bebe"]').element).toBe(document.activeElement)

    await tema.trigger('click')
    expect(pagina.find('fieldset').exists()).toBe(false)
    expect(pagina.find('nav').exists()).toBe(true)
  })

  it('aplica e salva o tema escolhido e mantém a seleção ao reabrir o menu', async () => {
    const pagina = await montar()
    const menu = pagina.get('[aria-label="Menu principal"]')
    await menu.trigger('click')
    await pagina.get('nav button').trigger('click')
    await pagina.get('input[value="verde"]').setValue(true)

    expect(document.documentElement.dataset.tema).toBe('verde')
    expect(localStorage.getItem('financas-pessoais:tema')).toBe('verde')
    expect((pagina.get('input[value="azul-bebe"]').element as HTMLInputElement).checked).toBe(false)
    expect(pagina.find('nav').exists()).toBe(true)
    expect(pagina.get('h1').text()).toBe('Categorias')

    await pagina.get('input[value="verde"]').trigger('keydown', { key: 'Escape' })
    await menu.trigger('click')
    expect(pagina.find('fieldset').exists()).toBe(false)
    await pagina.get('nav button').trigger('click')
    expect((pagina.get('input[value="verde"]').element as HTMLInputElement).checked).toBe(true)
    await pagina.get('input[value="azul-bebe"]').setValue(true)

    expect(document.documentElement.dataset.tema).toBe('azul-bebe')
    expect(localStorage.getItem('financas-pessoais:tema')).toBe('azul-bebe')
  })

  it('não fecha antes de registrar o clique no texto da opção de tema', async () => {
    const pagina = await montar()
    await pagina.get('[aria-label="Menu principal"]').trigger('click')
    await pagina.get('nav button').trigger('click')
    const azul = pagina.get('input[value="azul-bebe"]').element as HTMLInputElement
    const textoVerde = pagina.get('input[value="verde"]').element.nextElementSibling as HTMLElement

    // O clique no label pode tirar o foco do radio antes de ativar a nova opção.
    azul.blur()
    await flushPromises()
    expect(pagina.find('nav').exists()).toBe(true)
    textoVerde.click()
    await flushPromises()

    expect(document.documentElement.dataset.tema).toBe('verde')
    expect(localStorage.getItem('financas-pessoais:tema')).toBe('verde')
    expect((pagina.get('input[value="verde"]').element as HTMLInputElement).checked).toBe(true)
    expect(pagina.find('nav').exists()).toBe(true)
  })
})
