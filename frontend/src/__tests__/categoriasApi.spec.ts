import { afterEach, describe, expect, it, vi } from 'vitest'
import { categoriasApi } from '../services/categorias'

afterEach(() => vi.unstubAllGlobals())

describe('API de categorias', () => {
  it('envia o idioma da interface para a API', async () => {
    const buscar = vi.fn<typeof fetch>().mockResolvedValue(new Response('[]'))
    vi.stubGlobal('fetch', buscar)

    await categoriasApi.listar()

    expect(buscar).toHaveBeenCalledWith('/api/categorias', {
      headers: { Accept: 'application/json', 'Accept-Language': 'pt-BR' },
    })
  })

  it('apresenta erros de validação por campo', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            detail: 'Revise os campos informados.',
            erros: { nome: 'O nome é obrigatório.', descricao: 'Descrição muito longa.' },
          }),
          { status: 400 },
        ),
      ),
    )

    await expect(categoriasApi.criar({ nome: '', descricao: null, ativa: true })).rejects.toThrow(
      'O nome é obrigatório. Descrição muito longa.',
    )
  })

  it('apresenta uma mensagem útil quando o servidor devolve HTML em vez de JSON', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('<!doctype html><html lang="en"><title>Bad gateway</title></html>', {
          status: 502,
          headers: { 'Content-Type': 'text/html' },
        }),
      ),
    )

    await expect(categoriasApi.listar()).rejects.toThrow('Não foi possível concluir a operação.')
  })
})
