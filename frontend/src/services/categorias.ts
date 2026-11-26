import { requisitar } from './http'
import { cabecalhoCsrf } from './sessao'

export interface Categoria {
  id: number
  nome: string
  descricao: string | null
  ativa: boolean
}

export type CategoriaInput = Omit<Categoria, 'id'>

const rota = '/api/categorias'

export const categoriasApi = {
  listar: () => requisitar<Categoria[]>('categorias.listar', rota),
  buscar: (id: number) => requisitar<Categoria>('categorias.buscar', `${rota}/${id}`),
  criar: async (dados: CategoriaInput) =>
    requisitar<Categoria>('categorias.criar', rota, {
      method: 'POST',
      headers: await cabecalhoCsrf(),
      body: JSON.stringify(dados),
    }),
  atualizar: async (id: number, dados: CategoriaInput) =>
    requisitar<Categoria>('categorias.atualizar', `${rota}/${id}`, {
      method: 'PUT',
      headers: await cabecalhoCsrf(),
      body: JSON.stringify(dados),
    }),
  excluir: async (id: number) =>
    requisitar<void>('categorias.excluir', `${rota}/${id}`, {
      method: 'DELETE',
      headers: await cabecalhoCsrf(),
    }),
}
