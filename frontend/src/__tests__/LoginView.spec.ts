import { afterEach, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { i18n } from '@/i18n'
import App from '../App.vue'
import router from '../router'

const usuario = { id: 1, email: 'ana@example.com', criadoEm: '2026-11-25T12:00:00Z' }
const token = { headerName: 'X-CSRF-TOKEN', token: 'token-de-teste' }
let wrapper: VueWrapper | undefined

afterEach(() => {
  wrapper?.unmount()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

async function montar(caminho: string) {
  await router.replace(caminho)
  wrapper = mount(App, { global: { plugins: [router, i18n] } })
  await flushPromises()
  return wrapper
}

it('permite cadastrar, entrar nas categorias com a sessão e sair', async () => {
  let autenticada = false
  const buscar = vi.fn<typeof fetch>(async (rota, opcoes) => {
    if (rota === '/api/usuarios') return Response.json(usuario, { status: 201 })
    if (rota === '/api/sessao/csrf') return Response.json(token)
    if (rota === '/api/categorias') return Response.json([])
    if (rota === '/api/sessao' && opcoes?.method) {
      autenticada = opcoes.method === 'POST'
      return new Response(null, { status: 204 })
    }
    return autenticada ? Response.json(usuario) : new Response(null, { status: 401 })
  })
  vi.stubGlobal('fetch', buscar)

  const pagina = await montar('/')
  expect(router.currentRoute.value.path).toBe('/login')
  expect(pagina.get('h1').text()).toBe('Entrar na sua conta')

  await pagina.get('.alternar-acesso button').trigger('click')
  await pagina.get('#email-acesso').setValue(usuario.email)
  await pagina.get('#senha-acesso').setValue('Senha+1')
  await pagina.get('form').trigger('submit')
  expect(pagina.get('#erro-senha-acesso').text()).toBe('Use de 8 a 128 caracteres.')
  expect(buscar).not.toHaveBeenCalledWith('/api/usuarios', expect.anything())

  await pagina.get('#senha-acesso').setValue('Senha sem símbolo 7')
  await pagina.get('form').trigger('submit')
  expect(pagina.get('#erro-senha-acesso').text()).toContain('um símbolo')
  expect(buscar).not.toHaveBeenCalledWith('/api/usuarios', expect.anything())

  const senha = 'Senha+17'
  await pagina.get('#senha-acesso').setValue(senha)
  await pagina.get('form').trigger('submit')
  await flushPromises()
  expect(buscar).toHaveBeenCalledWith(
    '/api/usuarios',
    expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ email: usuario.email, senha }),
    }),
  )
  expect(pagina.get('[role="status"]').text()).toContain('Conta criada')
  expect((pagina.get('#email-acesso').element as HTMLInputElement).value).toBe(usuario.email)
  expect((pagina.get('#senha-acesso').element as HTMLInputElement).value).toBe('')

  await pagina.get('#senha-acesso').setValue(senha)
  await pagina.get('form').trigger('submit')
  await flushPromises()
  expect(buscar).toHaveBeenCalledWith(
    '/api/sessao',
    expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        'Accept-Language': 'pt-BR',
        [token.headerName]: token.token,
      }),
      body: 'email=ana%40example.com&senha=Senha%2B17',
    }),
  )
  expect(pagina.get('h1').text()).toBe('Categorias')

  await pagina.get('.header-actions .sair').trigger('click')
  await flushPromises()
  expect(buscar).toHaveBeenCalledWith(
    '/api/sessao',
    expect.objectContaining({
      method: 'DELETE',
      headers: expect.objectContaining({ [token.headerName]: token.token }),
    }),
  )
  expect(pagina.get('h1').text()).toBe('Entrar na sua conta')
})

it('mostra credenciais inválidas e permite tentar novamente sem emitir erro no console', async () => {
  vi.stubGlobal(
    'fetch',
    vi
      .fn<typeof fetch>()
      .mockImplementation(async (rota) =>
        rota === '/api/sessao/csrf' ? Response.json(token) : new Response(null, { status: 401 }),
      ),
  )
  const log = vi.spyOn(console, 'error').mockImplementation(() => {})

  const pagina = await montar('/login')
  await pagina.get('#email-acesso').setValue(usuario.email)
  await pagina.get('#senha-acesso').setValue('credencial de teste')
  await pagina.get('form').trigger('submit')
  await flushPromises()

  expect(pagina.get('[role="alert"]').text()).toBe('E-mail ou senha inválidos.')
  expect(pagina.get('button[type="submit"]').attributes('disabled')).toBeUndefined()
  expect(log).not.toHaveBeenCalled()
})
