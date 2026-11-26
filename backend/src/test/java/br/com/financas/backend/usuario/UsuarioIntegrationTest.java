package br.com.financas.backend.usuario;

import java.nio.charset.StandardCharsets;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class UsuarioIntegrationTest {

    private static final String SENHA = "Senha+17";
    private static final String NOVA_SENHA = "Outra!21";

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
    void salvaSomenteOHashEPermiteAdministrarAPropriaConta() throws Exception {
        Long id = criar("  Ana@example.com  ");
        String hash = jdbc.queryForObject("SELECT senha_hash FROM usuarios WHERE id = ?", String.class, id);
        assertThat(hash).startsWith("$argon2id$");
        assertThat(senhas.matches(SENHA, hash)).isTrue();

        var credenciais = credenciais("ana@EXAMPLE.COM", SENHA);
        mvc.perform(get("/api/usuarios/{id}", id).headers(credenciais))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("Ana@example.com"));
        var csrf = csrf(credenciais);
        mvc.perform(put("/api/usuarios/{id}", id).headers(credenciais).cookie(csrf.cookie())
                        .header(csrf.cabecalho(), csrf.token()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"novo@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("novo@example.com"));
        assertThat(jdbc.queryForObject("SELECT senha_hash FROM usuarios WHERE id = ?", String.class, id)).isEqualTo(hash);

        mvc.perform(put("/api/usuarios/{id}", id).headers(credenciais("novo@example.com", SENHA)).cookie(csrf.cookie())
                        .header(csrf.cabecalho(), csrf.token()).contentType(APPLICATION_JSON)
                        .content(json("novo@example.com", NOVA_SENHA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.senhaHash").doesNotExist());
        mvc.perform(get("/api/usuarios/{id}", id).headers(credenciais("novo@example.com", SENHA)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("AUTENTICACAO_NECESSARIA"));

        var credenciaisNovas = credenciais("novo@example.com", NOVA_SENHA);
        mvc.perform(delete("/api/usuarios/{id}", id).headers(credenciaisNovas).cookie(csrf.cookie())
                        .header(csrf.cabecalho(), csrf.token())).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Long.class)).isZero();
    }

    @Test
    void exigeAutenticacaoContaCorretaETokenCsrf() throws Exception {
        Long id = criar("ana@example.com");
        Long outroId = criar("bia@example.com");
        var credenciais = credenciais("ana@example.com", SENHA);
        var csrf = csrf(credenciais);

        mvc.perform(get("/api/usuarios/{id}", id)).andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(jsonPath("$.title").value("Autenticação necessária"));
        mvc.perform(delete("/api/usuarios/{id}", id).headers(credenciais))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mvc.perform(get("/api/usuarios/{id}", outroId).headers(credenciais))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("USUARIO_NAO_ENCONTRADO"));
        mvc.perform(put("/api/usuarios/{id}", outroId).headers(credenciais).cookie(csrf.cookie())
                        .header(csrf.cabecalho(), csrf.token()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"outro@example.com\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/usuarios/{id}", outroId).headers(credenciais).cookie(csrf.cookie())
                        .header(csrf.cabecalho(), csrf.token())).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT email FROM usuarios WHERE id = ?", String.class, outroId))
                .isEqualTo("bia@example.com");
    }

    @Test
    void validaCadastroEGaranteUnicidadeDoEmailNoBanco() throws Exception {
        criar("ana@example.com");
        Long outroId = criar("bia@example.com");
        cadastrar("ANA@example.com", SENHA).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_EMAIL_DUPLICADO"));
        cadastrar("inválido", "Senha+1").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists())
                .andExpect(jsonPath("$.erros.senha").value("A senha deve ter entre 8 e 128 caracteres."));

        var credenciais = credenciais("bia@example.com", SENHA);
        var csrf = csrf(credenciais);
        mvc.perform(put("/api/usuarios/{id}", outroId).headers(credenciais).cookie(csrf.cookie())
                        .header(csrf.cabecalho(), csrf.token()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"ANA@example.com\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("USUARIO_EMAIL_DUPLICADO"));

        assertThatThrownBy(() -> jdbc.update("INSERT INTO usuarios (email, senha_hash) VALUES (?, ?)",
                "ANA@example.com", "hash-de-teste"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageNotContaining("ANA@example.com");
    }

    @Test
    void iniciaMantemEEncerraSessaoComProtecaoCsrf() throws Exception {
        Long id = criar("ana@example.com");
        mvc.perform(get("/api/sessao")).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
        mvc.perform(post("/api/sessao").contentType(APPLICATION_FORM_URLENCODED))
                .andExpect(status().isForbidden());

        var respostaCsrf = mvc.perform(get("/api/sessao/csrf")).andExpect(status().isOk()).andReturn();
        var sessao = (MockHttpSession) respostaCsrf.getRequest().getSession(false);
        String idAnonimo = sessao.getId();
        String cabecalho = JsonPath.read(respostaCsrf.getResponse().getContentAsString(), "$.headerName");
        String token = JsonPath.read(respostaCsrf.getResponse().getContentAsString(), "$.token");

        mvc.perform(post("/api/sessao").session(sessao).header(cabecalho, token)
                        .contentType(APPLICATION_FORM_URLENCODED).param("email", "ana@example.com")
                        .param("senha", "credencial incorreta"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("AUTENTICACAO_NECESSARIA"));
        mvc.perform(get("/api/sessao").session(sessao)).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/sessao").session(sessao).header(cabecalho, token)
                        .contentType(APPLICATION_FORM_URLENCODED).param("email", "ANA@example.com").param("senha", SENHA)
                        .with(requisicao -> {
                            requisicao.setRequestedSessionId(idAnonimo);
                            requisicao.setRequestedSessionIdValid(true);
                            return requisicao;
                        }))
                .andExpect(status().isNoContent());
        assertThat(sessao.getId()).isNotEqualTo(idAnonimo);
        mvc.perform(get("/api/sessao").session(sessao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.senhaHash").doesNotExist());

        mvc.perform(delete("/api/sessao").session(sessao).header(cabecalho, token))
                .andExpect(status().isForbidden());
        var novoCsrf = mvc.perform(get("/api/sessao/csrf").session(sessao)).andExpect(status().isOk()).andReturn();
        String novoToken = JsonPath.read(novoCsrf.getResponse().getContentAsString(), "$.token");
        mvc.perform(delete("/api/sessao").session(sessao).header(cabecalho, novoToken))
                .andExpect(status().isNoContent());
        assertThat(sessao.isInvalid()).isTrue();
        mvc.perform(get("/api/sessao")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"senha minúscula 1!", "Senha sem número!", "Senha sem símbolo 1"})
    void confirmaFormatoDaSenhaNoCadastroENaAtualizacao(String senhaInvalida) throws Exception {
        String mensagem = "A senha deve incluir pelo menos uma letra maiúscula, um número e um símbolo.";
        cadastrar("ana@example.com", senhaInvalida).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.senha").value(mensagem));

        Long id = criar("ana@example.com");
        var credenciais = credenciais("ana@example.com", SENHA);
        var csrf = csrf(credenciais);
        mvc.perform(put("/api/usuarios/{id}", id).headers(credenciais).cookie(csrf.cookie())
                        .header(csrf.cabecalho(), csrf.token()).contentType(APPLICATION_JSON)
                        .content(json("ana@example.com", senhaInvalida)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.senha").value(mensagem));
    }

    private Long criar(String email) throws Exception {
        var resposta = cadastrar(email, SENHA).andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.criadoEm").exists())
                .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andReturn().getResponse();
        Number id = JsonPath.read(resposta.getContentAsString(), "$.id");
        assertThat(resposta.getHeader(HttpHeaders.LOCATION)).endsWith("/api/usuarios/" + id);
        return id.longValue();
    }

    private ResultActions cadastrar(String email, String senha) throws Exception {
        return mvc.perform(post("/api/usuarios").contentType(APPLICATION_JSON).content(json(email, senha)));
    }

    private String json(String email, String senha) {
        return "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha);
    }

    private HttpHeaders credenciais(String email, String senha) {
        var headers = new HttpHeaders();
        headers.setBasicAuth(email, senha, StandardCharsets.UTF_8);
        return headers;
    }

    private Csrf csrf(HttpHeaders credenciais) throws Exception {
        var resposta = mvc.perform(get("/api/usuarios/csrf").headers(credenciais)).andExpect(status().isOk())
                .andReturn().getResponse();
        return new Csrf(resposta.getCookie("XSRF-TOKEN"), JsonPath.read(resposta.getContentAsString(), "$.headerName"),
                JsonPath.read(resposta.getContentAsString(), "$.token"));
    }

    private record Csrf(Cookie cookie, String cabecalho, String token) {
    }
}
