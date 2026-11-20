# Orientações do projeto

- Monorepositório: `backend/` com Java 21, Spring Boot 4.x e Maven Wrapper; `frontend/` com Vue 3, TypeScript e Vite; PostgreSQL 18 em `compose.yaml`.
- Mantenha o escopo em categorias genéricas, compartilhadas entre receitas e despesas, sem campo de tipo. Não adicione autenticação, outras entidades ou microsserviços sem solicitação.
- Backend: organize por funcionalidade em `br.com.financas.backend`; use controller REST, DTOs com Bean Validation, service transacional e Spring Data JPA. Não exponha entidades diretamente.
- Execute Maven dentro de `backend/`; `.mvn/maven.config` seleciona `.mvn/settings.xml` para usar Maven Central sem alterar as configurações corporativas globais do usuário.
- Evolua o banco apenas com novas migrations em `backend/src/main/resources/db/migration`. Não edite migrations já aplicadas; mantenha `ddl-auto=validate`.
- Garanta unicidade de nomes de categoria sem distinguir maiúsculas de minúsculas, também no banco. Use 201 com Location na criação, 204 na exclusão e erros 400/404/409 em português.
- Frontend: Composition API com `<script setup lang="ts">`; chamadas HTTP em `src/services/`; telas responsivas, acessíveis e em português. Use `/api` com o proxy Vite.
- Tradução no frontend: centralize todo texto da interface em `frontend/src/i18n/locales/pt-BR.json`, incluindo títulos, botões, placeholders, estados, mensagens, nomes de temas, metadados e rótulos de acessibilidade. Use Vue I18n com `useI18n()` nos componentes e `i18n.global.t()` fora deles. Use chaves semânticas por funcionalidade e frases completas com parâmetros nomeados (`{nome}`); não concatene trechos traduzidos. Envie o locale atual no cabeçalho `Accept-Language` das chamadas HTTP.
- Tradução no backend: centralize mensagens públicas em `backend/src/main/resources/messages.properties` (português padrão, UTF-8), resolvidas pelo `MessageSource` no tratamento HTTP. Em Bean Validation use referências como `{categoria.nome.obrigatorio}` e parâmetros como `{max}`; em mensagens do `MessageSource` use `{0}`, `{1}`. Exceções de negócio devem carregar dados, sem frases de apresentação. Mantenha os códigos estáveis de erro em `ProblemDetail.codigo`, além de `title`, `detail` e `erros` traduzidos. O `IdiomaConfiguration` define os locales aceitos e o fallback `pt-BR`; não dependa do idioma do sistema operacional. Dados cadastrados pelo usuário não são traduzidos.
- Centralize as cores em `frontend/src/assets/theme.css`; use as variáveis CSS nos estilos globais e nos componentes. Os temas `azul-bebe` e `verde` são aplicados por `data-tema` no elemento `html`; use `aplicarTema` de `frontend/src/theme.ts` para alterná-los.
- Nunca versione `.env`, senhas, dependências ou arquivos gerados. Documente variáveis sem segredos em `.env.example`; preserve os lockfiles e o Maven Wrapper.
- Verifique alterações com `cd backend && ./mvnw verify` e, no frontend, `npm run test:unit -- --run`, `npm run build` e `npm run lint`. Testes de integração exigem Docker e usam banco descartável via Testcontainers; informe testes pulados e limitações.
- Atualize o README quando mudar comandos ou configuração. Não publique no GitHub sem autorização explícita.

## Logs no backend

- Use SLF4J com o logger da classe e campos estruturados via `addKeyValue`. Mantenha a mensagem estável e inclua só os metadados necessários, como `categoriaId`. O `RequisicaoLogFilter` já disponibiliza o `requestId` no MDC durante a chamada HTTP.
- Registre eventos de negócio quando ajudarem a investigar ou confirmar um resultado relevante, como identificar qual registro foi salvo. Para registrar uma gravação confirmada, emita o evento após o commit da transação.
- Use `INFO` para resultados relevantes, `WARN` para condições anormais que merecem atenção, `ERROR` para falhas inesperadas e `DEBUG` para diagnóstico detalhado. Aproveite o log HTTP para status esperados e mantenha um único registro por falha.
- Preserve a privacidade usando identificadores e outros metadados técnicos. Evite incluir dados de formulário, valores de parâmetros SQL, corpos de requisição ou credenciais.
- Para investigar comandos SQL, use o logger nativo do Hibernate com `LOG_NIVEL_SQL=DEBUG`. O padrão do projeto é `INFO`.
