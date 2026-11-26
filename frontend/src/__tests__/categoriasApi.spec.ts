import { Capacitor, CapacitorHttp } from '@capacitor/core'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { categoriasApi } from '../services/categorias'

vi.mock('@capacitor/core', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@capacitor/core')>()),
  CapacitorHttp: { request: vi.fn<typeof CapacitorHttp.request>() },
}))

afterEach(() => {
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
  vi.restoreAllMocks()
  vi.mocked(CapacitorHttp.request).mockReset()
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

  it('envia JSON e idioma pelo HTTP nativo e apresenta o conflito da API', async () => {
    vi.spyOn(Capacitor, 'isNativePlatform').mockReturnValue(true)
    vi.stubEnv('VITE_API_URL', 'http://10.0.2.2:8080/')
    const url = 'http://10.0.2.2:8080/api/categorias'
    const dados = { nome: 'Mercado', descricao: null, ativa: true }
    const requisitar = vi.mocked(CapacitorHttp.request).mockResolvedValue({
      status: 409,
      headers: { 'Content-Type': 'application/problem+json' },
      data: '{"detail":"Já existe uma categoria com esse nome."}',
      url,
    })

    await expect(categoriasApi.criar(dados)).rejects.toThrow('Já existe uma categoria com esse nome.')
    expect(requisitar).toHaveBeenCalledExactlyOnceWith({
      url,
      method: 'POST',
      data: JSON.stringify(dados),
      headers: {
        Accept: 'application/json',
        'Accept-Language': 'pt-BR',
        'Content-Type': 'application/json',
      },
    })
  })

  it('aceita uma exclusão sem corpo pelo HTTP nativo', async () => {
    vi.spyOn(Capacitor, 'isNativePlatform').mockReturnValue(true)
    vi.stubEnv('VITE_API_URL', 'http://10.0.2.2:8080')
    const url = 'http://10.0.2.2:8080/api/categorias/42'
    const requisitar = vi.mocked(CapacitorHttp.request).mockResolvedValue({
      status: 204,
      headers: {},
      data: '',
      url,
    })

    await expect(categoriasApi.excluir(42)).resolves.toBeUndefined()
    expect(requisitar).toHaveBeenCalledExactlyOnceWith({
      url,
      method: 'DELETE',
      data: undefined,
      headers: { Accept: 'application/json', 'Accept-Language': 'pt-BR' },
    })
  })
})
