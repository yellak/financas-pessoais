import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import type { Categoria } from '../services/categorias'
import CategoriasView from '../views/CategoriasView.vue'
import { i18n } from '@/i18n'

const mercado = { id: 1, nome: 'Mercado', descricao: 'Compras do mês', ativa: true }
const buscar = vi.fn<typeof fetch>()
let wrapper: VueWrapper | undefined
const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/categorias', component: CategoriasView },
    { path: '/login', name: 'login', component: { template: '<div />' } },
  ],
})

function responder(dados: unknown, status = 200) {
  return new Response(JSON.stringify(dados), { status })
}

async function montar(categorias: Categoria[] = [mercado]) {
  buscar.mockResolvedValueOnce(responder(categorias))
  wrapper = mount(CategoriasView, { attachTo: document.body, global: { plugins: [i18n, router] } })
  await flushPromises()
  return wrapper
}

// O jsdom não implementa os métodos nativos de dialog.
beforeAll(() => {
  Object.defineProperties(HTMLDialogElement.prototype, {
    showModal: {
      configurable: true,
      value(this: HTMLDialogElement) {
        this.open = true
      },
    },
    close: {
      configurable: true,
      value(this: HTMLDialogElement) {
        this.open = false
      },
    },
  })
})

beforeEach(async () => {
  buscar.mockReset()
  vi.stubGlobal('fetch', (rota: string, opcoes: RequestInit) =>
    rota === '/api/sessao/csrf'
      ? Promise.resolve(Response.json({ headerName: 'X-CSRF-TOKEN', token: 'token-teste' }))
      : buscar(rota, opcoes),
  )
  await router.replace('/categorias')
})

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  document.body.innerHTML = ''
  vi.unstubAllGlobals()
})

describe('Categorias', () => {
  it('mostra carregamento enquanto espera a API', () => {
    buscar.mockReturnValueOnce(new Promise(() => {}))
    wrapper = mount(CategoriasView, { global: { plugins: [i18n, router] } })

    expect(wrapper.get('[role="status"]').text()).toBe('Carregando categorias…')
    expect(wrapper.get('.page-heading button').attributes('disabled')).toBeDefined()
    expect(wrapper.find('dialog').exists()).toBe(false)
  })

  it('lista categorias com descrição e status', async () => {
    const pagina = await montar()

    expect(buscar).toHaveBeenCalledWith('/api/categorias', expect.any(Object))
    expect(pagina.get('.category-list').text()).toContain('Mercado')
    expect(pagina.get('.category-list').text()).toContain('Compras do mês')
    expect(pagina.get('.badge').text()).toBe('Ativa')
    expect(pagina.find('form').exists()).toBe(false)
    expect(pagina.find('dialog').exists()).toBe(false)
  })

  it('mostra um estado vazio', async () => {
    const pagina = await montar([])

    expect(pagina.text()).toContain('Nenhuma categoria cadastrada')
    expect(pagina.find('.category-list').exists()).toBe(false)
  })

  it('permite tentar novamente quando a conexão falha', async () => {
    buscar.mockRejectedValueOnce(new TypeError('Failed to fetch'))
    wrapper = mount(CategoriasView, { global: { plugins: [i18n, router] } })
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('Não foi possível conectar ao servidor')

    buscar.mockResolvedValueOnce(responder([mercado]))
    await wrapper.get('.panel-heading button').trigger('click')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.get('.category-list').text()).toContain('Mercado')
  })

  it('retorna ao login quando a sessão expira durante a atualização da lista', async () => {
    const pagina = await montar()
    buscar.mockResolvedValueOnce(new Response(null, { status: 401 }))

    await pagina.get('.panel-heading button').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.sessao).toBe('expirada')
  })

  it('cadastra categoria ativa, envia JSON e limpa o formulário', async () => {
    const pagina = await montar([])
    buscar.mockResolvedValueOnce(responder(mercado, 201))
    await pagina.get('.page-heading button').trigger('click')
    expect(pagina.get('dialog h2').text()).toBe('Nova categoria')
    await pagina.get('#nome').setValue('  Mercado  ')
    await pagina.get('#descricao').setValue('Compras do mês')
    await pagina.get('form').trigger('submit')
    await flushPromises()

    expect(buscar).toHaveBeenLastCalledWith(
      '/api/categorias',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'token-teste' }),
        body: JSON.stringify({ nome: 'Mercado', descricao: 'Compras do mês', ativa: true }),
      }),
    )
    expect(pagina.get('[role="status"]').text()).toBe('Categoria cadastrada com sucesso.')
    expect(pagina.find('dialog').exists()).toBe(false)
    expect(pagina.findAll('.category-item')).toHaveLength(1)
    await pagina.get('.page-heading button').trigger('click')
    expect((pagina.get('#nome').element as HTMLInputElement).value).toBe('')
  })

  it('rejeita nome composto apenas por espaços sem chamar a API', async () => {
    const pagina = await montar([])
    await pagina.get('.page-heading button').trigger('click')
    await pagina.get('#nome').setValue('   ')
    await pagina.get('form').trigger('submit')

    expect(pagina.get('[role="alert"]').text()).toBe('O nome é obrigatório.')
    expect(buscar).toHaveBeenCalledTimes(1)
  })

  it('preserva os campos e mostra conflito retornado pela API', async () => {
    const pagina = await montar()
    buscar.mockResolvedValueOnce(
      responder({ detail: 'Já existe uma categoria com esse nome.' }, 409),
    )
    await pagina.get('.page-heading button').trigger('click')
    await pagina.get('#nome').setValue('Mercado')
    await pagina.get('form').trigger('submit')
    await flushPromises()

    expect(pagina.get('dialog [role="alert"]').text()).toBe(
      'Já existe uma categoria com esse nome.',
    )
    expect((pagina.get('#nome').element as HTMLInputElement).value).toBe('Mercado')
    expect(pagina.findAll('.category-item')).toHaveLength(1)
  })

  it('edita a categoria incluindo o status e atualiza a lista', async () => {
    const pagina = await montar()
    buscar.mockResolvedValueOnce(responder({ ...mercado, nome: 'Alimentação', ativa: false }))
    await pagina.get('[aria-label="Editar Mercado"]').trigger('click')
    expect(pagina.get('dialog h2').text()).toBe('Editar categoria')
    expect((pagina.get('#nome').element as HTMLInputElement).value).toBe('Mercado')
    await pagina.get('#nome').setValue('Alimentação')
    await pagina.get('#ativa').setValue(false)
    await pagina.get('form').trigger('submit')
    await flushPromises()

    expect(buscar).toHaveBeenLastCalledWith(
      '/api/categorias/1',
      expect.objectContaining({ method: 'PUT' }),
    )
    expect(pagina.get('.category-list').text()).toContain('Alimentação')
    expect(pagina.get('.badge').text()).toBe('Inativa')
    expect(pagina.get('[role="status"]').text()).toBe('Categoria atualizada com sucesso.')
    expect(pagina.find('dialog').exists()).toBe(false)
  })

  it('cancela edição sem alterar a categoria', async () => {
    const pagina = await montar()
    await pagina.get('[aria-label="Editar Mercado"]').trigger('click')
    await pagina.get('#nome').setValue('Outro nome')
    await pagina.get('dialog .secondary').trigger('click')

    expect(pagina.find('dialog').exists()).toBe(false)
    expect(pagina.get('.category-list').text()).toContain('Mercado')
    expect(buscar).toHaveBeenCalledTimes(1)
  })

  it('visualiza os detalhes em uma janela sem campos editáveis', async () => {
    const pagina = await montar()
    await pagina.get('[aria-label="Visualizar Mercado"]').trigger('click')

    const janela = pagina.get('dialog[open]')
    expect(janela.get('h2').text()).toBe('Detalhes da categoria')
    expect(janela.text()).toContain('Mercado')
    expect(janela.text()).toContain('Compras do mês')
    expect(janela.get('.badge').text()).toBe('Ativa')
    expect(janela.find('input, textarea, form').exists()).toBe(false)
    await janela.get('.category-details button').trigger('click')
    expect(pagina.find('dialog').exists()).toBe(false)
    expect(buscar).toHaveBeenCalledTimes(1)
  })

  it('visualiza categoria inativa sem descrição', async () => {
    const pagina = await montar([{ ...mercado, descricao: null, ativa: false }])
    await pagina.get('[aria-label="Visualizar Mercado"]').trigger('click')

    expect(pagina.get('dialog').text()).toContain('Sem descrição')
    expect(pagina.get('dialog .badge').text()).toBe('Inativa')
  })

  it('foca o formulário ao abrir e devolve o foco ao fechar com Esc', async () => {
    const pagina = await montar()
    const botao = pagina.get('.page-heading button')
    ;(botao.element as HTMLButtonElement).focus()
    await botao.trigger('click')

    expect(document.activeElement).toBe(pagina.get('#nome').element)
    expect(document.documentElement.style.overflow).toBe('hidden')
    await pagina.get('dialog').trigger('cancel')

    expect(pagina.find('dialog').exists()).toBe(false)
    expect(document.activeElement).toBe(botao.element)
    expect(document.documentElement.style.overflow).toBe('')
  })

  it('fecha pelo botão superior e descarta alterações não salvas', async () => {
    const pagina = await montar()
    await pagina.get('[aria-label="Editar Mercado"]').trigger('click')
    await pagina.get('#nome').setValue('Nome não salvo')
    await pagina.get('[aria-label="Fechar janela"]').trigger('click')
    await pagina.get('[aria-label="Editar Mercado"]').trigger('click')

    expect((pagina.get('#nome').element as HTMLInputElement).value).toBe('Mercado')
    expect(buscar).toHaveBeenCalledTimes(1)
  })

  it('impede fechamento e envio duplicado enquanto salva', async () => {
    const pagina = await montar([])
    let concluir!: (resposta: Response) => void
    buscar.mockReturnValueOnce(
      new Promise((resolve) => {
        concluir = resolve
      }),
    )
    await pagina.get('.page-heading button').trigger('click')
    await pagina.get('#nome').setValue('Mercado')
    await pagina.get('form').trigger('submit')

    expect(pagina.get('dialog').attributes('aria-busy')).toBe('true')
    expect(pagina.get('button[type="submit"]').text()).toBe('Salvando…')
    expect(pagina.get('[aria-label="Fechar janela"]').attributes('disabled')).toBeDefined()
    expect(pagina.get('dialog .secondary').attributes('disabled')).toBeDefined()
    await pagina.get('dialog').trigger('cancel')
    await pagina.get('form').trigger('submit')
    expect(pagina.find('dialog[open]').exists()).toBe(true)
    expect(buscar).toHaveBeenCalledTimes(2)

    concluir(responder(mercado, 201))
    await flushPromises()
    expect(pagina.find('dialog').exists()).toBe(false)
    expect(pagina.findAll('.category-item')).toHaveLength(1)
  })

  it('aguarda confirmação e permite cancelar a exclusão', async () => {
    const pagina = await montar()
    await pagina.get('[aria-label="Excluir Mercado"]').trigger('click')
    expect(pagina.get('.delete-confirmation').text()).toContain('Esta ação é definitiva')
    expect(pagina.get('.delete-confirmation p').text()).toBe(
      'Excluir “Mercado”? Esta ação é definitiva.',
    )
    expect(pagina.get('.delete-confirmation').attributes('aria-label')).toBe(
      'Confirmar exclusão de Mercado',
    )
    await pagina.get('.delete-confirmation .secondary').trigger('click')

    expect(pagina.find('.delete-confirmation').exists()).toBe(false)
    expect(buscar).toHaveBeenCalledTimes(1)
  })

  it('exclui após confirmar e aceita resposta 204 sem JSON', async () => {
    const pagina = await montar()
    buscar.mockResolvedValueOnce(new Response(null, { status: 204 }))
    await pagina.get('[aria-label="Excluir Mercado"]').trigger('click')
    await pagina.get('.delete-confirmation .danger').trigger('click')
    await flushPromises()

    expect(buscar).toHaveBeenLastCalledWith(
      '/api/categorias/1',
      expect.objectContaining({ method: 'DELETE' }),
    )
    expect(pagina.find('.category-item').exists()).toBe(false)
    expect(pagina.get('[role="status"]').text()).toBe('Categoria excluída com sucesso.')
  })

  it('mantém a categoria na lista se a exclusão falhar', async () => {
    const pagina = await montar()
    buscar.mockResolvedValueOnce(responder({ detail: 'Não foi possível excluir.' }, 409))
    await pagina.get('[aria-label="Excluir Mercado"]').trigger('click')
    await pagina.get('.delete-confirmation .danger').trigger('click')
    await flushPromises()

    expect(pagina.findAll('.category-item')).toHaveLength(1)
    expect(pagina.get('[role="alert"]').text()).toBe('Não foi possível excluir.')
  })
})
