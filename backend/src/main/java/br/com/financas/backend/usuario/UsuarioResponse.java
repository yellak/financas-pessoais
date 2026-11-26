package br.com.financas.backend.usuario;

import java.time.Instant;

public record UsuarioResponse(Long id, String email, Instant criadoEm) {

    static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getEmail(), usuario.getCriadoEm());
    }
}
