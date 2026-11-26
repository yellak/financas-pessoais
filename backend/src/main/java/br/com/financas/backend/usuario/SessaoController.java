package br.com.financas.backend.usuario;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessao")
public class SessaoController {

    private final UsuarioService service;

    public SessaoController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public UsuarioResponse atual(@AuthenticationPrincipal UsuarioAutenticado autenticado, HttpServletRequest requisicao) {
        try {
            return service.buscar(autenticado.id());
        } catch (UsuarioNaoEncontradoException exception) {
            var sessao = requisicao.getSession(false);
            if (sessao != null) {
                sessao.invalidate();
            }
            throw new CredentialsExpiredException(null);
        }
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }
}
