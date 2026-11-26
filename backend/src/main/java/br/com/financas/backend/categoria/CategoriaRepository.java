package br.com.financas.backend.categoria;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByUsuarioId(Long usuarioId, Sort ordem);

    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Query("select c.id from Categoria c where c.usuarioId = :usuarioId and lower(c.nome) = lower(:nome)")
    Optional<Long> buscarIdPorUsuarioENome(Long usuarioId, String nome);
}
