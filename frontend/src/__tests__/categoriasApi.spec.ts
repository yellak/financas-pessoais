import { afterEach, describe, expect, it, vi } from 'vitest'
import { categoriasApi } from '../services/categorias'

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

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
    const log = vi.spyOn(console, 'error').mockImplementation(() => {})
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
    expect(log).not.toHaveBeenCalled()
  })

  it('registra falha 5xx com o ID da resposta e mantém a mensagem da interface', async () => {
    const log = vi.spyOn(console, 'error').mockImplementation(() => {})
    const requestId = '584e6f7e-a740-4ca2-a8b9-bdb9506c7c06'
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('<!doctype html><html lang="en"><title>Bad gateway</title></html>', {
          status: 502,
          headers: { 'Content-Type': 'text/html', 'X-Request-ID': requestId },
        }),
      ),
    )

    await expect(categoriasApi.listar()).rejects.toThrow('Não foi possível concluir a operação.')
    expect(log).toHaveBeenCalledExactlyOnceWith('[frontend]', {
      evento: 'falha_api',
      operacao: 'categorias.listar',
      status: 502,
      requestId,
    })
  })

  it('registra a operação de uma falha de rede sem copiar dados da requisição', async () => {
    const log = vi.spyOn(console, 'error').mockImplementation(() => {})
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Detalhe privado')))

    await expect(
      categoriasApi.atualizar(42, { nome: 'Nome privado', descricao: null, ativa: true }),
    ).rejects.toThrow('Não foi possível conectar ao servidor')
    expect(log).toHaveBeenCalledExactlyOnceWith('[frontend]', {
      evento: 'falha_rede',
      operacao: 'categorias.atualizar',
    })
  })

  it('registra uma resposta de sucesso com JSON inválido', async () => {
    const log = vi.spyOn(console, 'error').mockImplementation(() => {})
    const resposta = new Response('<html lang="pt-BR">Detalhe privado</html>')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta))

    await expect(categoriasApi.listar()).rejects.toThrow('Não foi possível concluir a operação.')
    expect(log).toHaveBeenCalledExactlyOnceWith('[frontend]', {
      evento: 'resposta_api_invalida',
      operacao: 'categorias.listar',
      status: 200,
      requestId: null,
    })
  })
})
