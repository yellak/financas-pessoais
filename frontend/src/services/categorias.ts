import { i18n } from '@/i18n'

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

async function requisitar<T>(caminho = '', opcoes: RequestInit = {}): Promise<T> {
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
    throw new Error(i18n.global.t('erros.conexao'))
  }

  if (!resposta.ok) {
    const problema = (await resposta.json().catch(() => null)) as ProblemaApi | null
    const campos = problema?.erros ? Object.values(problema.erros).join(' ') : ''
    throw new Error(campos || problema?.detail || i18n.global.t('erros.operacao'))
  }

  if (resposta.status === 204) return undefined as T
  return resposta.json() as Promise<T>
}

export const categoriasApi = {
  listar: () => requisitar<Categoria[]>(),
  buscar: (id: number) => requisitar<Categoria>(`/${id}`),
  criar: (dados: CategoriaInput) =>
    requisitar<Categoria>('', { method: 'POST', body: JSON.stringify(dados) }),
  atualizar: (id: number, dados: CategoriaInput) =>
    requisitar<Categoria>(`/${id}`, { method: 'PUT', body: JSON.stringify(dados) }),
  excluir: (id: number) => requisitar<void>(`/${id}`, { method: 'DELETE' }),
}
