package br.com.financas.backend.categoria;

import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    public List<CategoriaResponse> listar(Long usuarioId) {
        return repository.findAllByUsuarioId(usuarioId, Sort.by("nome", "id")).stream()
                .map(CategoriaResponse::de).toList();
    }

    public CategoriaResponse buscar(Long usuarioId, Long id) {
        return CategoriaResponse.de(encontrar(usuarioId, id));
    }

    @Transactional
    public CategoriaResponse criar(Long usuarioId, CategoriaRequest request) {
        verificarNomeDisponivel(usuarioId, request.nome(), null);
        Categoria categoria = new Categoria(usuarioId, request.nome(), request.descricao(), request.ativa());
        return CategoriaResponse.de(salvar(categoria));
    }

    @Transactional
    public CategoriaResponse atualizar(Long usuarioId, Long id, CategoriaRequest request) {
        Categoria categoria = encontrar(usuarioId, id);
        verificarNomeDisponivel(usuarioId, request.nome(), id);
        categoria.atualizar(request.nome(), request.descricao(), request.ativa());
        return CategoriaResponse.de(salvar(categoria));
    }

    @Transactional
    public void excluir(Long usuarioId, Long id) {
        repository.delete(encontrar(usuarioId, id));
        repository.flush();
    }

    private void verificarNomeDisponivel(Long usuarioId, String nome, Long id) {
        if (repository.buscarIdPorUsuarioENome(usuarioId, nome).filter(existente -> !existente.equals(id)).isPresent()) {
            throw new NomeCategoriaDuplicadoException();
        }
    }

    private Categoria salvar(Categoria categoria) {
        try {
            return repository.saveAndFlush(categoria);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException violacao) {
                if ("uk_categorias_usuario_nome".equals(violacao.getConstraintName())) {
                    throw new NomeCategoriaDuplicadoException();
                }
                if ("fk_categorias_usuario".equals(violacao.getConstraintName())) {
                    throw new CredentialsExpiredException(null);
                }
            }
            throw exception;
        }
    }

    private Categoria encontrar(Long usuarioId, Long id) {
        return repository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new CategoriaNaoEncontradaException(id));
    }
}
