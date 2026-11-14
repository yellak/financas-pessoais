package br.com.financas.backend.categoria;

public class NomeCategoriaDuplicadoException extends RuntimeException {

    public NomeCategoriaDuplicadoException() {
        super("Já existe uma categoria com esse nome.");
    }
}
