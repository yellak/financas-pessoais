package br.com.financas.backend.categoria;

import com.jayway.jsonpath.JsonPath;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CategoriaIntegrationTest {

    private static final String SENHA = "credencial temporária de teste";

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
    private PasswordEncoder senhas;

    @BeforeEach
    void limpar() {
        jdbc.update("DELETE FROM usuarios");
    }

    @Test
    void executaCrudDaContaComMigrationNoPostgres() throws Exception {
        Conta conta = entrar("ana@example.com");
        requisitar(get("/api/categorias"), conta).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        var resposta = requisitar(post("/api/categorias").contentType(APPLICATION_JSON).content("{\"nome\":\"  Mercado  \"}"), conta)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Mercado"))
                .andExpect(jsonPath("$.ativa").value(true)).andExpect(jsonPath("$.usuarioId").doesNotExist())
                .andReturn().getResponse();

        Number id = JsonPath.read(resposta.getContentAsString(), "$.id");
        assertThat(jdbc.queryForObject("SELECT usuario_id FROM categorias WHERE id = ?", Long.class, id.longValue()))
                .isEqualTo(conta.id());
        requisitar(get("/api/categorias/{id}", id), conta).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        requisitar(put("/api/categorias/{id}", id).contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"Alimentação\",\"descricao\":\"Compras\",\"ativa\":false}"), conta)
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativa").value(false));
        requisitar(put("/api/categorias/{id}", id).contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"ALIMENTAÇÃO\",\"ativa\":false}"), conta)
                .andExpect(status().isOk());
        requisitar(get("/api/categorias"), conta).andExpect(jsonPath("$.length()").value(1));
        requisitar(delete("/api/categorias/{id}", id), conta).andExpect(status().isNoContent());
        requisitar(get("/api/categorias/{id}", id), conta).andExpect(status().isNotFound());
    }

    @Test
    void isolaContasEAplicaUnicidadeSomenteDentroDeCadaConta() throws Exception {
        Conta ana = entrar("ana@example.com");
        Conta bia = entrar("bia@example.com");
        Long idAna = criarCategoria(ana, "Mercado");
        Long idBia = criarCategoria(bia, "MERCADO");
        Long lazer = criarCategoria(bia, "Lazer");

        requisitar(get("/api/categorias"), ana).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(idAna));
        requisitar(get("/api/categorias/{id}", idAna), bia).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_NAO_ENCONTRADA"));
        requisitar(put("/api/categorias/{id}", idAna).contentType(APPLICATION_JSON)
                .content("{\"nome\":\"Transporte\"}"), bia).andExpect(status().isNotFound());
        requisitar(delete("/api/categorias/{id}", idAna), bia).andExpect(status().isNotFound());
        requisitar(get("/api/categorias/{id}", idAna), ana).andExpect(jsonPath("$.nome").value("Mercado"));
        requisitar(get("/api/categorias/{id}", idBia), bia).andExpect(jsonPath("$.nome").value("MERCADO"));

        requisitar(post("/api/categorias").contentType(APPLICATION_JSON).content("{\"nome\":\" mercado \"}"), ana)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CATEGORIA_NOME_DUPLICADO"));
        requisitar(put("/api/categorias/{id}", lazer).contentType(APPLICATION_JSON)
                .content("{\"nome\":\"Mercado\"}"), bia).andExpect(status().isConflict());
    }

    @Test
    void exigeSessaoECsrfEInvalidaSessaoDaContaExcluida() throws Exception {
        mvc.perform(get("/api/categorias")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUTENTICACAO_NECESSARIA"));
        mvc.perform(post("/api/categorias").contentType(APPLICATION_JSON).content("{\"nome\":\"Mercado\"}"))
                .andExpect(status().isUnauthorized());

        Conta ana = entrar("ana@example.com");
        Conta bia = entrar("bia@example.com");
        Long idAna = criarCategoria(ana, "Mercado");
        Long idBia = criarCategoria(bia, "Lazer");
        mvc.perform(delete("/api/categorias/{id}", idAna).session(ana.sessao()))
                .andExpect(status().isForbidden());

        jdbc.update("DELETE FROM usuarios WHERE id = ?", ana.id());
        requisitar(get("/api/categorias"), ana).andExpect(status().isUnauthorized());
        assertThat(ana.sessao().isInvalid()).isTrue();
        requisitar(get("/api/categorias/{id}", idBia), bia).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM categorias WHERE id = ?", Long.class, idAna)).isZero();
    }

    @Test
    void bancoGaranteProprietarioUnicidadePorContaEAtivaPorPadrao() {
        Long ana = inserirUsuario("ana@example.com");
        Long bia = inserirUsuario("bia@example.com");
        jdbc.update("INSERT INTO categorias (usuario_id, nome) VALUES (?, ?)", ana, "Mercado");
        jdbc.update("INSERT INTO categorias (usuario_id, nome) VALUES (?, ?)", bia, "MERCADO");
        assertThat(jdbc.queryForObject("SELECT ativa FROM categorias WHERE usuario_id = ?", Boolean.class, ana)).isTrue();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO categorias (usuario_id, nome) VALUES (?, ?)", ana, "MERCADO"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO categorias (nome) VALUES (?)", "Sem proprietário"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO categorias (usuario_id, nome) VALUES (?, ?)", -1L, "Órfã"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void migraVersaoAnteriorComCategoriasPreservandoUsuarios() {
        String schema = "teste_migracao_categorias";
        var configuracao = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema);
        configuracao.target("2").load().migrate();
        jdbc.update("INSERT INTO teste_migracao_categorias.categorias (nome) VALUES (?)", "Legada");
        jdbc.update("INSERT INTO teste_migracao_categorias.usuarios (email, senha_hash) VALUES (?, ?)",
                "ana@example.com", senhas.encode(SENHA));

        configuracao.target("latest").load().migrate();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM teste_migracao_categorias.categorias", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT email FROM teste_migracao_categorias.usuarios", String.class))
                .isEqualTo("ana@example.com");
    }

    private Long inserirUsuario(String email) {
        return jdbc.queryForObject("INSERT INTO usuarios (email, senha_hash) VALUES (?, ?) RETURNING id",
                Long.class, email, senhas.encode(SENHA));
    }

    private Conta entrar(String email) throws Exception {
        Long id = inserirUsuario(email);
        var sessao = new MockHttpSession();
        var csrf = csrf(sessao);
        mvc.perform(post("/api/sessao").session(sessao).header(csrf.cabecalho(), csrf.token())
                        .contentType(APPLICATION_FORM_URLENCODED).param("email", email).param("senha", SENHA))
                .andExpect(status().isNoContent());
        return new Conta(id, sessao, csrf(sessao));
    }

    private Csrf csrf(MockHttpSession sessao) throws Exception {
        var resposta = mvc.perform(get("/api/sessao/csrf").session(sessao)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Csrf(JsonPath.read(resposta, "$.headerName"), JsonPath.read(resposta, "$.token"));
    }

    private Long criarCategoria(Conta conta, String nome) throws Exception {
        var resposta = requisitar(post("/api/categorias").contentType(APPLICATION_JSON)
                        .content("{\"nome\":\"%s\"}".formatted(nome)), conta)
                .andExpect(status().isCreated()).andReturn().getResponse();
        Number id = JsonPath.read(resposta.getContentAsString(), "$.id");
        return id.longValue();
    }

    private ResultActions requisitar(MockHttpServletRequestBuilder requisicao, Conta conta) throws Exception {
        return mvc.perform(requisicao.session(conta.sessao()).header(conta.csrf().cabecalho(), conta.csrf().token()));
    }

    private record Conta(Long id, MockHttpSession sessao, Csrf csrf) {
    }

    private record Csrf(String cabecalho, String token) {
    }
}
