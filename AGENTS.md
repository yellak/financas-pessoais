# Orientações do projeto

- Monorepositório: `backend/` com Java 21, Spring Boot 4.x e Maven Wrapper; `frontend/` com Vue 3, TypeScript e Vite; PostgreSQL 18 em `compose.yaml`.
- Mantenha o escopo em categorias genéricas, compartilhadas entre receitas e despesas, sem campo de tipo. Não adicione autenticação, outras entidades ou microsserviços sem solicitação.
- Backend: organize por funcionalidade em `br.com.financas.backend`; use controller REST, DTOs com Bean Validation, service transacional e Spring Data JPA. Não exponha entidades diretamente.
- Execute Maven dentro de `backend/`; `.mvn/maven.config` seleciona `.mvn/settings.xml` para usar Maven Central sem alterar as configurações corporativas globais do usuário.
- Evolua o banco apenas com novas migrations em `backend/src/main/resources/db/migration`. Não edite migrations já aplicadas; mantenha `ddl-auto=validate`.
- Garanta unicidade de nomes de categoria sem distinguir maiúsculas de minúsculas, também no banco. Use 201 com Location na criação, 204 na exclusão e erros 400/404/409 em português.
- Frontend: Composition API com `<script setup lang="ts">`; chamadas HTTP em `src/services/`; telas responsivas, acessíveis e em português. Use `/api` com o proxy Vite.
- Nunca versione `.env`, senhas, dependências ou arquivos gerados. Documente variáveis sem segredos em `.env.example`; preserve os lockfiles e o Maven Wrapper.
- Verifique alterações com `cd backend && ./mvnw verify` e, no frontend, `npm run test:unit -- --run`, `npm run build` e `npm run lint`. Testes de integração exigem Docker e usam banco descartável via Testcontainers; informe testes pulados e limitações.
- Atualize o README quando mudar comandos ou configuração. Não publique no GitHub sem autorização explícita.
