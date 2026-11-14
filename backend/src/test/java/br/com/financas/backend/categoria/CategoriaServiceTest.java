package br.com.financas.backend.categoria;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository repository;

    private CategoriaService service;

    @BeforeEach
    void setUp() {
        service = new CategoriaService(repository);
    }

    @Test
    void criaNormalizandoCamposEAtivandoPorPadrao() {
        when(repository.saveAndFlush(any(Categoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoriaResponse resposta = service.criar(new CategoriaRequest("  Mercado  ", "  Compras  ", null));

        assertThat(resposta.nome()).isEqualTo("Mercado");
        assertThat(resposta.descricao()).isEqualTo("Compras");
        assertThat(resposta.ativa()).isTrue();
    }

    @Test
    void rejeitaNomeDuplicadoNaCriacao() {
        when(repository.existsByNomeIgnoreCase("Mercado")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new CategoriaRequest("Mercado", null, true)))
                .isInstanceOf(NomeCategoriaDuplicadoException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void atualizaCamposIncluindoStatusInativoEDescricaoVazia() {
        Categoria categoria = new Categoria("Mercado", "Compras", true);
        when(repository.findById(1L)).thenReturn(Optional.of(categoria));
        when(repository.saveAndFlush(categoria)).thenReturn(categoria);

        CategoriaResponse resposta = service.atualizar(1L, new CategoriaRequest("Alimentação", "  ", false));

        assertThat(resposta.nome()).isEqualTo("Alimentação");
        assertThat(resposta.descricao()).isNull();
        assertThat(resposta.ativa()).isFalse();
        verify(repository).existsByNomeIgnoreCaseAndIdNot("Alimentação", 1L);
    }

    @Test
    void rejeitaNomeDeOutraCategoriaNaAtualizacao() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Categoria("Mercado", null, true)));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Transporte", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, new CategoriaRequest("Transporte", null, true)))
                .isInstanceOf(NomeCategoriaDuplicadoException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void listaTambemCategoriasInativasEmOrdemDeNome() {
        when(repository.findAll(Sort.by("nome", "id")))
                .thenReturn(List.of(new Categoria("Mercado", null, false)));

        assertThat(service.listar()).extracting(CategoriaResponse::ativa).containsExactly(false);
    }

    @Test
    void buscaInexistenteRetornaErro() {
        assertThatThrownBy(() -> service.buscar(99L)).isInstanceOf(CategoriaNaoEncontradaException.class);
    }

    @Test
    void atualizacaoInexistenteRetornaErro() {
        assertThatThrownBy(() -> service.atualizar(99L, new CategoriaRequest("Mercado", null, true)))
                .isInstanceOf(CategoriaNaoEncontradaException.class);
    }

    @Test
    void exclusaoInexistenteRetornaErroSemExcluir() {
        assertThatThrownBy(() -> service.excluir(99L)).isInstanceOf(CategoriaNaoEncontradaException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void excluiCategoriaExistente() {
        Categoria categoria = new Categoria("Mercado", null, true);
        when(repository.findById(1L)).thenReturn(Optional.of(categoria));

        service.excluir(1L);

        verify(repository).delete(categoria);
        verify(repository).flush();
    }
}
