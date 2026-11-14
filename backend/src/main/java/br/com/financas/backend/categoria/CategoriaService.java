package br.com.financas.backend.categoria;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    public List<CategoriaResponse> listar() {
        return repository.findAll(Sort.by("nome", "id")).stream()
                .map(CategoriaResponse::de).toList();
    }

    public CategoriaResponse buscar(Long id) {
        return CategoriaResponse.de(encontrar(id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        if (repository.existsByNomeIgnoreCase(request.nome())) {
            throw new NomeCategoriaDuplicadoException();
        }
        Categoria categoria = new Categoria(request.nome(), request.descricao(), request.ativa());
        return CategoriaResponse.de(repository.saveAndFlush(categoria));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = encontrar(id);
        if (repository.existsByNomeIgnoreCaseAndIdNot(request.nome(), id)) {
            throw new NomeCategoriaDuplicadoException();
        }
        categoria.atualizar(request.nome(), request.descricao(), request.ativa());
        return CategoriaResponse.de(repository.saveAndFlush(categoria));
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(encontrar(id));
        repository.flush();
    }

    private Categoria encontrar(Long id) {
        return repository.findById(id).orElseThrow(() -> new CategoriaNaoEncontradaException(id));
    }
}
