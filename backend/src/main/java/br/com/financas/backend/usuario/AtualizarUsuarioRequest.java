package br.com.financas.backend.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequest(
        @NotBlank(message = "{usuario.email.obrigatorio}")
        @Email(regexp = "[^:]+", message = "{usuario.email.invalido}")
        @Size(max = 254, message = "{usuario.email.tamanho}")
        String email,
        @Size(min = PoliticaSenha.TAMANHO_MINIMO, max = PoliticaSenha.TAMANHO_MAXIMO, message = "{usuario.senha.tamanho}")
        @Pattern(regexp = PoliticaSenha.FORMATO, message = "{usuario.senha.formato}")
        String senha) {

    public AtualizarUsuarioRequest {
        email = email == null ? null : email.strip();
    }

    @Override
    public String toString() {
        return "AtualizarUsuarioRequest";
    }
}
