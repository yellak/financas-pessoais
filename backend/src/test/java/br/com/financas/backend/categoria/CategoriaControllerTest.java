package br.com.financas.backend.categoria;

import java.util.List;

import br.com.financas.backend.common.ApiExceptionHandler;
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
@Import(ApiExceptionHandler.class)
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
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nome").exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejeitaNomeEDescricaoMuitoLongos() throws Exception {
        String json = "{\"nome\":\"" + "a".repeat(101) + "\",\"descricao\":\"" + "b".repeat(501) + "\"}";
        mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.descricao").exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejeitaJsonMalformado() throws Exception {
        mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void rejeitaIdInvalido() throws Exception {
        mvc.perform(get("/api/categorias/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void categoriaAusenteRetorna404() throws Exception {
        when(service.buscar(99L)).thenThrow(new CategoriaNaoEncontradaException(99L));
        mvc.perform(get("/api/categorias/99"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Categoria de ID 99 não encontrada."));
    }

    @Test
    void nomeDuplicadoRetorna409() throws Exception {
        when(service.criar(any())).thenThrow(new NomeCategoriaDuplicadoException());
        mvc.perform(post("/api/categorias").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Mercado\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Já existe uma categoria com esse nome."));
    }

    @Test
    void concorrenciaNoBancoRetorna409SemExporSql() throws Exception {
        when(service.criar(any())).thenThrow(new DataIntegrityViolationException("SQL privado"));
        mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Mercado\"}"))
                .andExpect(status().isConflict())
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
