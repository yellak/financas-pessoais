package br.com.financas.backend.categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String descricao,
        Boolean ativa) {

    public CategoriaRequest {
        nome = nome == null ? null : nome.strip();
        descricao = descricao == null || descricao.isBlank() ? null : descricao.strip();
        ativa = ativa == null ? Boolean.TRUE : ativa;
    }
}
