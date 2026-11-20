import { i18n } from '@/i18n'
import { registrarErro } from '@/logger'

export interface Categoria {
  id: number
  nome: string
  descricao: string | null
  ativa: boolean
}

export type CategoriaInput = Omit<Categoria, 'id'>

interface ProblemaApi {
  detail?: string
  erros?: Record<string, string>
}

async function requisitar<T>(operacao: string, caminho = '', opcoes: RequestInit = {}): Promise<T> {
  let resposta: Response
  try {
    resposta = await fetch(`/api/categorias${caminho}`, {
      ...opcoes,
      headers: {
        Accept: 'application/json',
        'Accept-Language': i18n.global.locale.value,
        ...(opcoes.body ? { 'Content-Type': 'application/json' } : {}),
      },
    })
  } catch {
    registrarErro({ evento: 'falha_rede', operacao })
    throw new Error(i18n.global.t('erros.conexao'))
  }

  if (!resposta.ok) {
    if (resposta.status >= 500) {
      registrarErro({
        evento: 'falha_api',
        operacao,
        status: resposta.status,
        requestId: resposta.headers.get('X-Request-ID'),
      })
    }
    const problema = (await resposta.json().catch(() => null)) as ProblemaApi | null
    const campos = problema?.erros ? Object.values(problema.erros).join(' ') : ''
    throw new Error(campos || problema?.detail || i18n.global.t('erros.operacao'))
  }

  if (resposta.status === 204) return undefined as T
  try {
    return (await resposta.json()) as T
  } catch {
    registrarErro({
      evento: 'resposta_api_invalida',
      operacao,
      status: resposta.status,
      requestId: resposta.headers.get('X-Request-ID'),
    })
    throw new Error(i18n.global.t('erros.operacao'))
  }
}

export const categoriasApi = {
  listar: () => requisitar<Categoria[]>('categorias.listar'),
  buscar: (id: number) => requisitar<Categoria>('categorias.buscar', `/${id}`),
  criar: (dados: CategoriaInput) =>
    requisitar<Categoria>('categorias.criar', '', { method: 'POST', body: JSON.stringify(dados) }),
  atualizar: (id: number, dados: CategoriaInput) =>
    requisitar<Categoria>('categorias.atualizar', `/${id}`, {
      method: 'PUT',
      body: JSON.stringify(dados),
    }),
  excluir: (id: number) => requisitar<void>('categorias.excluir', `/${id}`, { method: 'DELETE' }),
}
