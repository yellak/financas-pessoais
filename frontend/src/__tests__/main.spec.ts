import { afterEach, expect, it, vi } from 'vitest'

vi.mock('../App.vue', async () => {
  const { h } = await import('vue')
  return {
    default: {
      render: () =>
        h('button', {
          onClick() {
            throw new TypeError('Detalhe privado')
          },
        }),
    },
  }
})

afterEach(() => {
  document.getElementById('app')?.remove()
  history.replaceState(null, '', '/')
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

it('registra um erro do Vue sem copiar a mensagem e a query da página', async () => {
  const log = vi.spyOn(console, 'error').mockImplementation(() => {})
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json({ id: 1 })))
  history.replaceState(null, '', '/categorias?filtro=privado')
  const destino = document.createElement('div')
  destino.id = 'app'
  document.body.append(destino)

  await import('../main')
  const { default: router } = await import('../router')
  await router.isReady()
  expect(router.currentRoute.value.query.filtro).toBe('privado')
  destino.querySelector('button')?.click()

  expect(log).toHaveBeenCalledExactlyOnceWith('[frontend]', {
    evento: 'erro_vue',
    origem: 'native event handler',
    rota: '/categorias',
    tipoErro: 'TypeError',
  })
})
