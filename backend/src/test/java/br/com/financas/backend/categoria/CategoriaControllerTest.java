package br.com.financas.backend.categoria;

import java.util.List;

import br.com.financas.backend.common.ApiExceptionHandler;
import br.com.financas.backend.common.IdiomaConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoriaController.class)
@Import({ApiExceptionHandler.class, IdiomaConfiguration.class})
class CategoriaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CategoriaService service;

    @Test
    void listaCategorias() throws Exception {
        when(service.listar()).thenReturn(List.of(new CategoriaResponse(1L, "Mercado", null, true)));
        mvc.perform(get("/api/categorias"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Mercado"));
    }

    @Test
    void buscaCategoria() throws Exception {
        when(service.buscar(1L)).thenReturn(new CategoriaResponse(1L, "Mercado", null, true));
        mvc.perform(get("/api/categorias/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void criaComLocationEStatus201() throws Exception {
        when(service.criar(any())).thenReturn(new CategoriaResponse(1L, "Mercado", null, true));
        mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Mercado"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/categorias/1"))
                .andExpect(jsonPath("$.ativa").value(true));
        verify(service).criar(new CategoriaRequest("Mercado", null, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"nome\":null}", "{\"nome\":\"   \"}"})
    void rejeitaNomeAusenteOuEmBranco(String json) throws Exception {
        mvc.perform(post("/api/categorias").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Revise os campos informados."))
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.erros.nome").value("O nome é obrigatório."));
        verifyNoInteractions(service);
    }

    @Test
    void rejeitaNomeEDescricaoMuitoLongos() throws Exception {
        String json = "{\"nome\":\"" + "a".repeat(101) + "\",\"descricao\":\"" + "b".repeat(501) + "\"}";
        mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").value("O nome deve ter no máximo 100 caracteres."))
                .andExpect(jsonPath("$.erros.descricao").value("A descrição deve ter no máximo 500 caracteres."));
        verifyNoInteractions(service);
    }

    @Test
    void rejeitaJsonMalformado() throws Exception {
        mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail").value("Verifique o JSON e os tipos dos valores informados."))
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    void rejeitaIdInvalido() throws Exception {
        mvc.perform(get("/api/categorias/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void categoriaAusenteRetorna404() throws Exception {
        when(service.buscar(99L)).thenThrow(new CategoriaNaoEncontradaException(99L));
        mvc.perform(get("/api/categorias/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Categoria não encontrada"))
                .andExpect(jsonPath("$.detail").value("Categoria de ID 99 não encontrada."))
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_NAO_ENCONTRADA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pt-BR", "en-US", "fr-FR,pt;q=0.8"})
    void mantemPortuguesParaIdiomasAindaNaoSuportados(String idioma) throws Exception {
        when(service.buscar(1234L)).thenThrow(new CategoriaNaoEncontradaException(1234L));
        mvc.perform(get("/api/categorias/1234").header("Accept-Language", idioma))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Categoria de ID 1234 não encontrada."))
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_NAO_ENCONTRADA"));
    }

    @Test
    void nomeDuplicadoRetorna409() throws Exception {
        when(service.criar(any())).thenThrow(new NomeCategoriaDuplicadoException());
        mvc.perform(post("/api/categorias").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Mercado\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Nome já utilizado"))
                .andExpect(jsonPath("$.detail").value("Já existe uma categoria com esse nome."))
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_NOME_DUPLICADO"));
    }

    @Test
    void concorrenciaNoBancoRetorna409SemExporSql() throws Exception {
        when(service.criar(any())).thenThrow(new DataIntegrityViolationException("SQL privado"));
        mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Mercado\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de dados"))
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DADOS"))
                .andExpect(jsonPath("$.detail").value("Não foi possível concluir a operação. Verifique se o nome já está em uso."));
    }

    @Test
    void atualizaCategoria() throws Exception {
        when(service.atualizar(eq(1L), any())).thenReturn(new CategoriaResponse(1L, "Lazer", "Passeios", false));
        mvc.perform(put("/api/categorias/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Lazer\",\"descricao\":\"Passeios\",\"ativa\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativa").value(false));
    }

    @Test
    void validaTambemNaAtualizacao() throws Exception {
        mvc.perform(put("/api/categorias/1").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void excluiSemCorpoNaResposta() throws Exception {
        mvc.perform(delete("/api/categorias/1"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).excluir(1L);
    }

    @Test
    void excluirAusenteRetorna404() throws Exception {
        doThrow(new CategoriaNaoEncontradaException(99L)).when(service).excluir(99L);
        mvc.perform(delete("/api/categorias/99")).andExpect(status().isNotFound());
    }
}
