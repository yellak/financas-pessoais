package br.com.financas.backend.categoria;

public record CategoriaResponse(Long id, String nome, String descricao, boolean ativa) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(),
                categoria.getDescricao(), categoria.isAtiva());
    }
}
