package br.com.financas.backend.categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "{categoria.nome.obrigatorio}")
        @Size(max = 100, message = "{categoria.nome.tamanho}")
        String nome,
        @Size(max = 500, message = "{categoria.descricao.tamanho}")
        String descricao,
        Boolean ativa) {

    public CategoriaRequest {
        nome = nome == null ? null : nome.strip();
        descricao = descricao == null || descricao.isBlank() ? null : descricao.strip();
        ativa = ativa == null ? Boolean.TRUE : ativa;
    }
}
