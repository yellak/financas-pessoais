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

    private static final Long USUARIO_ID = 7L;

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

        CategoriaResponse resposta = service.criar(USUARIO_ID, new CategoriaRequest("  Mercado  ", "  Compras  ", null));

        assertThat(resposta.nome()).isEqualTo("Mercado");
        assertThat(resposta.descricao()).isEqualTo("Compras");
        assertThat(resposta.ativa()).isTrue();
    }

    @Test
    void rejeitaNomeDuplicadoNaCriacao() {
        when(repository.buscarIdPorUsuarioENome(USUARIO_ID, "Mercado")).thenReturn(Optional.of(1L));

        assertThatThrownBy(() -> service.criar(USUARIO_ID, new CategoriaRequest("Mercado", null, true)))
                .isInstanceOf(NomeCategoriaDuplicadoException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void atualizaCamposIncluindoStatusInativoEDescricaoVazia() {
        Categoria categoria = new Categoria(USUARIO_ID, "Mercado", "Compras", true);
        when(repository.findByIdAndUsuarioId(1L, USUARIO_ID)).thenReturn(Optional.of(categoria));
        when(repository.saveAndFlush(categoria)).thenReturn(categoria);

        CategoriaResponse resposta = service.atualizar(USUARIO_ID, 1L, new CategoriaRequest("Alimentação", "  ", false));

        assertThat(resposta.nome()).isEqualTo("Alimentação");
        assertThat(resposta.descricao()).isNull();
        assertThat(resposta.ativa()).isFalse();
        verify(repository).buscarIdPorUsuarioENome(USUARIO_ID, "Alimentação");
    }

    @Test
    void rejeitaNomeDeOutraCategoriaNaAtualizacao() {
        when(repository.findByIdAndUsuarioId(1L, USUARIO_ID)).thenReturn(Optional.of(new Categoria(USUARIO_ID, "Mercado", null, true)));
        when(repository.buscarIdPorUsuarioENome(USUARIO_ID, "Transporte")).thenReturn(Optional.of(2L));

        assertThatThrownBy(() -> service.atualizar(USUARIO_ID, 1L, new CategoriaRequest("Transporte", null, true)))
                .isInstanceOf(NomeCategoriaDuplicadoException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void listaTambemCategoriasInativasEmOrdemDeNome() {
        when(repository.findAllByUsuarioId(USUARIO_ID, Sort.by("nome", "id")))
                .thenReturn(List.of(new Categoria(USUARIO_ID, "Mercado", null, false)));

        assertThat(service.listar(USUARIO_ID)).extracting(CategoriaResponse::ativa).containsExactly(false);
    }

    @Test
    void buscaInexistenteRetornaErro() {
        assertThatThrownBy(() -> service.buscar(USUARIO_ID, 99L)).isInstanceOf(CategoriaNaoEncontradaException.class);
    }

    @Test
    void atualizacaoInexistenteRetornaErro() {
        assertThatThrownBy(() -> service.atualizar(USUARIO_ID, 99L, new CategoriaRequest("Mercado", null, true)))
                .isInstanceOf(CategoriaNaoEncontradaException.class);
    }

    @Test
    void exclusaoInexistenteRetornaErroSemExcluir() {
        assertThatThrownBy(() -> service.excluir(USUARIO_ID, 99L)).isInstanceOf(CategoriaNaoEncontradaException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void excluiCategoriaExistente() {
        Categoria categoria = new Categoria(USUARIO_ID, "Mercado", null, true);
        when(repository.findByIdAndUsuarioId(1L, USUARIO_ID)).thenReturn(Optional.of(categoria));

        service.excluir(USUARIO_ID, 1L);

        verify(repository).delete(categoria);
        verify(repository).flush();
    }
}
