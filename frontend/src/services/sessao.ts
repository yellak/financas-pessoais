import { ErroApi, requisitar } from './http'

interface Credenciais {
  email: string
  senha: string
}

interface Usuario {
  id: number
  email: string
  criadoEm: string
}

interface TokenCsrf {
  headerName: string
  token: string
}

const rota = '/api/sessao'

export async function cabecalhoCsrf() {
  const { headerName, token } = await requisitar<TokenCsrf>('sessao.csrf', `${rota}/csrf`)
  return { [headerName]: token }
}

export const sessaoApi = {
  cadastrar: (credenciais: Credenciais) =>
    requisitar<Usuario>('usuarios.cadastrar', '/api/usuarios', {
      method: 'POST',
      body: JSON.stringify(credenciais),
    }),
  async atual() {
    try {
      return await requisitar<Usuario>('sessao.atual', rota)
    } catch (erro) {
      if (erro instanceof ErroApi && erro.status === 401) return null
      throw erro
    }
  },
  async entrar(credenciais: Credenciais) {
    const csrf = await cabecalhoCsrf()
    await requisitar<void>('sessao.entrar', rota, {
      method: 'POST',
      headers: { ...csrf, 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
      body: new URLSearchParams({ ...credenciais }).toString(),
    })
  },
  async sair() {
    await requisitar<void>('sessao.sair', rota, {
      method: 'DELETE',
      headers: await cabecalhoCsrf(),
    })
  },
}
