package br.com.financas.backend.categoria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CategoriaIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.config.import", () -> "");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CategoriaRepository repository;

    @BeforeEach
    void limpar() {
        repository.deleteAllInBatch();
    }

    @Test
    void executaCrudCompletoComMigrationNoPostgres() throws Exception {
        mvc.perform(get("/api/categorias")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/categorias").contentType(APPLICATION_JSON).content("{\"nome\":\"  Mercado  \"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Mercado"))
                .andExpect(jsonPath("$.ativa").value(true));

        Long id = repository.findAll().getFirst().getId();
        mvc.perform(get("/api/categorias/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(put("/api/categorias/{id}", id).contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Alimentação\",\"descricao\":\"Compras\",\"ativa\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativa").value(false));
        mvc.perform(put("/api/categorias/{id}", id).contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"ALIMENTAÇÃO\",\"ativa\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/categorias")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/api/categorias/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/categorias/{id}", id)).andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }

    @Test
    void rejeitaNomeDuplicadoSemDistinguirMaiusculasNaCriacaoEEdicao() throws Exception {
        repository.saveAndFlush(new Categoria("Mercado", null, true));
        Long id = repository.saveAndFlush(new Categoria("Lazer", null, true)).getId();

        mvc.perform(post("/api/categorias").contentType(APPLICATION_JSON).content("{\"nome\":\" mercado \"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/categorias/{id}", id).contentType(APPLICATION_JSON).content("{\"nome\":\"MERCADO\"}"))
                .andExpect(status().isConflict());
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void bancoGaranteUnicidadeMesmoSemConsultaPreviaEAtivaPorPadrao() {
        jdbc.update("INSERT INTO categorias (nome) VALUES (?)", "Mercado");
        assertThat(jdbc.queryForObject("SELECT ativa FROM categorias WHERE nome = ?", Boolean.class, "Mercado")).isTrue();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO categorias (nome) VALUES (?)", "MERCADO"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
