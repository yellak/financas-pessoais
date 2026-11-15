import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import CategoriasView from '../views/CategoriasView.vue'

const mercado = { id: 1, nome: 'Mercado', descricao: 'Compras do mês', ativa: true }
const buscar = vi.fn<typeof fetch>()
let wrapper: VueWrapper | undefined

function responder(dados: unknown, status = 200) {
  return new Response(JSON.stringify(dados), { status })
}

async function montar(categorias = [mercado]) {
  buscar.mockResolvedValueOnce(responder(categorias))
  wrapper = mount(CategoriasView)
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  buscar.mockReset()
  vi.stubGlobal('fetch', buscar)
})

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  vi.unstubAllGlobals()
})

describe('Categorias', () => {
  it('mostra carregamento enquanto espera a API', () => {
    buscar.mockReturnValueOnce(new Promise(() => {}))
    wrapper = mount(CategoriasView)

    expect(wrapper.get('[role="status"]').text()).toBe('Carregando categorias…')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
  })

  it('lista categorias com descrição e status', async () => {
    const pagina = await montar()

    expect(buscar).toHaveBeenCalledWith('/api/categorias', expect.any(Object))
    expect(pagina.get('.category-list').text()).toContain('Mercado')
    expect(pagina.get('.category-list').text()).toContain('Compras do mês')
    expect(pagina.get('.badge').text()).toBe('Ativa')
  })

  it('mostra um estado vazio', async () => {
    const pagina = await montar([])

    expect(pagina.text()).toContain('Nenhuma categoria cadastrada')
    expect(pagina.find('.category-list').exists()).toBe(false)
  })

  it('permite tentar novamente quando a conexão falha', async () => {
    buscar.mockRejectedValueOnce(new TypeError('Failed to fetch'))
    wrapper = mount(CategoriasView)
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('Não foi possível conectar ao servidor')

    buscar.mockResolvedValueOnce(responder([mercado]))
    await wrapper.get('.panel-heading button').trigger('click')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.get('.category-list').text()).toContain('Mercado')
  })

  it('cadastra categoria ativa, envia JSON e limpa o formulário', async () => {
    const pagina = await montar([])
    buscar.mockResolvedValueOnce(responder(mercado, 201))
    await pagina.get('#nome').setValue('  Mercado  ')
    await pagina.get('#descricao').setValue('Compras do mês')
    await pagina.get('form').trigger('submit')
    await flushPromises()

    expect(buscar).toHaveBeenLastCalledWith(
      '/api/categorias',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ nome: 'Mercado', descricao: 'Compras do mês', ativa: true }),
      }),
    )
    expect(pagina.get('[role="status"]').text()).toBe('Categoria cadastrada com sucesso.')
    expect((pagina.get('#nome').element as HTMLInputElement).value).toBe('')
    expect(pagina.findAll('.category-item')).toHaveLength(1)
  })

  it('rejeita nome composto apenas por espaços sem chamar a API', async () => {
    const pagina = await montar([])
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
    await pagina.get('#nome').setValue('Mercado')
    await pagina.get('form').trigger('submit')
    await flushPromises()

    expect(pagina.get('[role="alert"]').text()).toBe('Já existe uma categoria com esse nome.')
    expect((pagina.get('#nome').element as HTMLInputElement).value).toBe('Mercado')
    expect(pagina.findAll('.category-item')).toHaveLength(1)
  })

  it('edita a categoria incluindo o status e atualiza a lista', async () => {
    const pagina = await montar()
    buscar.mockResolvedValueOnce(responder({ ...mercado, nome: 'Alimentação', ativa: false }))
    await pagina.get('[aria-label="Editar Mercado"]').trigger('click')
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
  })

  it('cancela edição sem alterar a categoria', async () => {
    const pagina = await montar()
    await pagina.get('[aria-label="Editar Mercado"]').trigger('click')
    await pagina.get('#nome').setValue('Outro nome')
    const cancelar = pagina.findAll('button').find((botao) => botao.text() === 'Cancelar edição')!
    await cancelar.trigger('click')

    expect(pagina.get('#formulario-titulo').text()).toBe('Nova categoria')
    expect(pagina.get('.category-list').text()).toContain('Mercado')
    expect(buscar).toHaveBeenCalledTimes(1)
  })

  it('aguarda confirmação e permite cancelar a exclusão', async () => {
    const pagina = await montar()
    await pagina.get('[aria-label="Excluir Mercado"]').trigger('click')
    expect(pagina.get('.delete-confirmation').text()).toContain('Esta ação é definitiva')
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
