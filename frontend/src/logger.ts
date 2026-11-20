type EventoErro =
  | { evento: 'erro_vue'; origem: string; rota: string | undefined; tipoErro: string }
  | { evento: 'falha_rede'; operacao: string }
  | {
      evento: 'falha_api' | 'resposta_api_invalida'
      operacao: string
      status: number
      requestId: string | null
    }

export function registrarErro(evento: EventoErro) {
  console.error('[frontend]', evento)
}
