import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { aplicarTema, inicializarTema, temaAtual, temaPadrao } from '../theme'

beforeEach(() => localStorage.clear())

afterEach(() => {
  vi.restoreAllMocks()
  aplicarTema(temaPadrao)
  localStorage.clear()
  delete document.documentElement.dataset.tema
})

describe('Preferência de tema', () => {
  it('inicia com azul bebê quando não há preferência salva', () => {
    inicializarTema()

    expect(document.documentElement.dataset.tema).toBe('azul-bebe')
    expect(temaAtual.value).toBe('azul-bebe')
  })

  it('restaura o tema verde salvo neste navegador', () => {
    localStorage.setItem('financas-pessoais:tema', 'verde')

    inicializarTema()

    expect(document.documentElement.dataset.tema).toBe('verde')
    expect(temaAtual.value).toBe('verde')
  })

  it('usa o padrão quando a preferência salva é inválida', () => {
    localStorage.setItem('financas-pessoais:tema', 'tema-inexistente')

    inicializarTema()

    expect(document.documentElement.dataset.tema).toBe('azul-bebe')
    expect(temaAtual.value).toBe('azul-bebe')
    expect(localStorage.getItem('financas-pessoais:tema')).toBe('azul-bebe')
  })

  it('permite iniciar e trocar o tema quando o armazenamento está indisponível', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('Armazenamento indisponível', 'SecurityError')
    })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('Armazenamento indisponível', 'SecurityError')
    })

    expect(() => inicializarTema()).not.toThrow()
    expect(document.documentElement.dataset.tema).toBe('azul-bebe')
    expect(() => aplicarTema('verde')).not.toThrow()
    expect(document.documentElement.dataset.tema).toBe('verde')
    expect(temaAtual.value).toBe('verde')
  })
})
