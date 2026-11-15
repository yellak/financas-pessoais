import { readonly, ref } from 'vue'

export const temasDisponiveis = [
  { valor: 'azul-bebe', nome: 'Azul bebê' },
  { valor: 'verde', nome: 'Verde' },
] as const

export type Tema = (typeof temasDisponiveis)[number]['valor']

export const temaPadrao: Tema = 'azul-bebe'
const chaveTema = 'financas-pessoais:tema'
const estadoTema = ref<Tema>(temaPadrao)
export const temaAtual = readonly(estadoTema)

export function aplicarTema(tema: Tema) {
  document.documentElement.dataset.tema = tema
  estadoTema.value = tema
  try {
    localStorage.setItem(chaveTema, tema)
  } catch {
    // A troca continua funcionando quando o armazenamento está indisponível.
  }
}

export function inicializarTema() {
  let tema: Tema = temaPadrao
  try {
    const salvo = localStorage.getItem(chaveTema)
    const encontrado = temasDisponiveis.find((opcao) => opcao.valor === salvo)
    if (encontrado) tema = encontrado.valor
  } catch {
    // Usa o padrão quando não é possível ler a preferência deste navegador.
  }
  aplicarTema(tema)
}
