export const temasDisponiveis = [
  { valor: 'azul-bebe', nome: 'Azul bebê' },
  { valor: 'verde', nome: 'Verde' },
] as const

export type Tema = (typeof temasDisponiveis)[number]['valor']

export const temaPadrao: Tema = 'azul-bebe'

export function aplicarTema(tema: Tema) {
  document.documentElement.dataset.tema = tema
}
