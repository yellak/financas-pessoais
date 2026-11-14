package br.com.financas.backend.categoria;

public class CategoriaNaoEncontradaException extends RuntimeException {

    public CategoriaNaoEncontradaException(Long id) {
        super("Categoria de ID " + id + " não encontrada.");
    }
}
