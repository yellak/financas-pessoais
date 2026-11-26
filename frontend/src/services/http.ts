import { Capacitor, CapacitorHttp } from '@capacitor/core'
import { i18n } from '@/i18n'
import { registrarErro } from '@/logger'

interface ProblemaApi {
  detail?: string
  erros?: Record<string, string>
}

interface OpcoesRequisicao {
  method?: string
  body?: string
  headers?: Record<string, string>
}

export class ErroApi extends Error {
  constructor(
    readonly status: number,
    mensagem: string,
  ) {
    super(mensagem)
  }
}

export async function requisitar<T>(
  operacao: string,
  rota: string,
  opcoes: OpcoesRequisicao = {},
): Promise<T> {
  const headers = {
    Accept: 'application/json',
    'Accept-Language': i18n.global.locale.value,
    ...(opcoes.body ? { 'Content-Type': 'application/json' } : {}),
    ...opcoes.headers,
  }
  let resposta: Response
  try {
    if (Capacitor.isNativePlatform()) {
      const url = new URL(import.meta.env.VITE_API_URL ?? '')
      url.pathname = `${url.pathname.replace(/\/+$/, '')}${rota}`

      const {
        status,
        headers: headersResposta,
        data,
      } = await CapacitorHttp.request({
        url: url.href,
        method: opcoes.method,
        data: opcoes.body,
        headers,
      })
      const corpo = typeof data === 'string' ? data : JSON.stringify(data)
      resposta = new Response(status === 204 ? null : corpo || null, {
        status,
        headers: headersResposta,
      })
    } else {
      resposta = await fetch(rota, { ...opcoes, headers })
    }
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
    throw new ErroApi(
      resposta.status,
      campos || problema?.detail || i18n.global.t('erros.operacao'),
    )
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
