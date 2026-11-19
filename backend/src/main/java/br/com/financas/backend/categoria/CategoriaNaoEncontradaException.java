package br.com.financas.backend.categoria;

public class CategoriaNaoEncontradaException extends RuntimeException {

    private final Long id;

    public CategoriaNaoEncontradaException(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
