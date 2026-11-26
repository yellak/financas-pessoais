package br.com.financas.backend.usuario;

import java.util.List;

import org.springframework.security.core.userdetails.User;

public final class UsuarioAutenticado extends User {

    private final Long id;

    UsuarioAutenticado(Usuario usuario) {
        super(usuario.getEmail(), usuario.getSenhaHash(), List.of());
        this.id = usuario.getId();
    }

    public Long id() {
        return id;
    }
}
