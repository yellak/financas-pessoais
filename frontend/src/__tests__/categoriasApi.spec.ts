import { afterEach, describe, expect, it, vi } from 'vitest'
import { categoriasApi } from '../services/categorias'

afterEach(() => vi.unstubAllGlobals())

describe('API de categorias', () => {
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
      vi.fn().mockResolvedValue(new Response('<html>Bad gateway</html>', { status: 502 })),
    )

    await expect(categoriasApi.listar()).rejects.toThrow('Não foi possível concluir a operação.')
  })
})
