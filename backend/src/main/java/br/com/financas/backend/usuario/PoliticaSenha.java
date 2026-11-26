package br.com.financas.backend.usuario;

final class PoliticaSenha {

    static final int TAMANHO_MINIMO = 8;
    static final int TAMANHO_MAXIMO = 128;
    static final String FORMATO = "(?s)(?=.*\\p{Lu})(?=.*\\p{Nd})(?=.*[\\p{P}\\p{S}]).*";

    private PoliticaSenha() {
    }
}
