package br.com.financas.backend.usuario;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder senhas;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder senhas) {
        this.repository = repository;
        this.senhas = senhas;
    }

    @Transactional
    public UsuarioResponse criar(CriarUsuarioRequest request) {
        verificarEmailDisponivel(request.email(), null);
        Usuario usuario = salvar(new Usuario(request.email(), senhas.encode(request.senha())));
        Long usuarioId = usuario.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.atInfo().addKeyValue("usuarioId", usuarioId).log("Usuário cadastrado");
            }
        });
        return UsuarioResponse.de(usuario);
    }

    public UsuarioResponse buscar(Long id) {
        return repository.findById(id).map(UsuarioResponse::de).orElseThrow(UsuarioNaoEncontradoException::new);
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, AtualizarUsuarioRequest request) {
        Usuario usuario = encontrarParaAlterar(id);
        verificarEmailDisponivel(request.email(), id);
        usuario.alterarEmail(request.email());
        if (request.senha() != null) {
            usuario.alterarSenha(senhas.encode(request.senha()));
        }
        return UsuarioResponse.de(salvar(usuario));
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(encontrarParaAlterar(id));
        repository.flush();
    }

    private Usuario encontrarParaAlterar(Long id) {
        return repository.buscarParaAlterar(id).orElseThrow(UsuarioNaoEncontradoException::new);
    }

    private void verificarEmailDisponivel(String email, Long id) {
        if (repository.buscarPorEmail(email).filter(usuario -> !usuario.getId().equals(id)).isPresent()) {
            throw new EmailUsuarioDuplicadoException();
        }
    }

    private Usuario salvar(Usuario usuario) {
        try {
            return repository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException violacao
                    && "uk_usuarios_email".equals(violacao.getConstraintName())) {
                throw new EmailUsuarioDuplicadoException();
            }
            throw exception;
        }
    }
}
