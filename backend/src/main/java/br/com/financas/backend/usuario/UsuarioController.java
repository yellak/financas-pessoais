package br.com.financas.backend.usuario;

import java.net.URI;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest request) {
        UsuarioResponse usuario = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(usuario.id()).toUri();
        return ResponseEntity.created(location).body(usuario);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        verificarUsuario(id, autenticado);
        return service.buscar(id);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarUsuarioRequest request,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        verificarUsuario(id, autenticado);
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        verificarUsuario(id, autenticado);
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    private void verificarUsuario(Long id, UsuarioAutenticado autenticado) {
        if (!id.equals(autenticado.id())) {
            throw new UsuarioNaoEncontradoException();
        }
    }
}
