package br.com.financas.backend.categoria;

import java.net.URI;
import java.util.List;

import br.com.financas.backend.usuario.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoriaResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.listar(usuario.id());
    }

    @GetMapping("/{id}")
    public CategoriaResponse buscar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return service.buscar(usuario.id(), id);
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse categoria = service.criar(usuario.id(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(categoria.id()).toUri();
        return ResponseEntity.created(location).body(categoria);
    }

    @PutMapping("/{id}")
    public CategoriaResponse atualizar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest request) {
        return service.atualizar(usuario.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        service.excluir(usuario.id(), id);
        return ResponseEntity.noContent().build();
    }
}
