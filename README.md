# Finanças pessoais

CRUD de categorias em um monorepositório:

- `backend/`: Java 21, Spring Boot 4.1, Maven, Spring Data JPA e Flyway.
- `frontend/`: Vue 3, TypeScript, Vite e Vitest.
- `compose.yaml`: PostgreSQL 18 com volume persistente e healthcheck.

As categorias são genéricas: um único cadastro, como “Investimento” ou “Outros”, pode ser utilizado tanto para receitas quanto para despesas, sem campo de tipo na categoria.

## Pré-requisitos

- JDK 21 e `JAVA_HOME` apontando para ele.
- Node.js `^22.18.0` ou `>=24.12.0` e npm.
- Docker com Docker Compose e permissão para acessar o daemon.

O Maven Wrapper baixa o Maven; a primeira execução também precisa de internet para obter dependências. No Windows, use `mvnw.cmd` em vez de `./mvnw`.

Execute os comandos Maven dentro de `backend/`. O arquivo `backend/.mvn/maven.config` seleciona automaticamente `.mvn/settings.xml`, que usa o Maven Central por HTTPS. Assim, este projeto pessoal não depende dos espelhos corporativos configurados em `~/.m2/settings.xml`; a configuração global permanece intacta.

## Configuração local

Na raiz, se ainda não houver `.env`:

```bash
cp .env.example .env
```

Edite `.env` e substitua `DB_PASSWORD` por uma senha local. Não sobrescreva um `.env` já configurado. Esse arquivo não é versionado; `.env.example` contém apenas valores de exemplo.

| Variável | Padrão | Uso |
| --- | --- | --- |
| `DB_HOST` | `localhost` | Host do banco usado pelo backend |
| `DB_PORT` | `5432` | Porta local do banco |
| `DB_NAME` | `financas` | Nome do banco |
| `DB_USER` | `financas` | Usuário do banco |
| `DB_PASSWORD` | obrigatório | Senha local, sem valor padrão no backend ou Compose |
| `LOG_FORMATO_CONSOLE` | texto | Use `logstash` para emitir logs em JSON na saída padrão do backend |
| `LOG_NIVEL_SQL` | `INFO` | Use `DEBUG` para ver o SQL emitido pelo Hibernate |

O Docker Compose lê o `.env` da raiz. O backend também importa esse arquivo ao ser iniciado a partir da raiz ou de `backend/`; variáveis de ambiente exportadas têm precedência. Use entradas simples `CHAVE=valor`, compatíveis com arquivos de propriedades Java. Não coloque senhas em variáveis `VITE_*`, pois elas são expostas no navegador.

## Iniciar o banco

Na raiz:

```bash
docker compose up -d --wait postgres
docker compose ps
```

O PostgreSQL fica disponível apenas em `127.0.0.1:5432` por padrão. O volume `postgres_data` preserva os dados entre reinícios. Variáveis de usuário, banco e senha do PostgreSQL inicializam somente um volume novo; alterar o `.env` não muda credenciais de um banco já inicializado.

Para parar os serviços preservando os dados:

```bash
docker compose down
```

## Iniciar o backend

Em um terminal a partir da raiz:

```bash
cd backend
./mvnw spring-boot:run
```

API: `http://localhost:8080/api/categorias`. O Flyway aplica `V1__criar_categorias.sql` na inicialização; o Hibernate apenas valida o schema com `ddl-auto=validate`.

### Logs do backend

O backend gera um `X-Request-ID` para cada chamada à API e devolve o mesmo valor no cabeçalho da resposta. Procure esse ID na saída do backend para localizar o método, o padrão da rota, o status HTTP e a duração em milissegundos da chamada.

O nível é `INFO` para respostas de sucesso e erros esperados como `400`, `404` e `409`. Respostas `5xx` usam `ERROR` e incluem o tipo e a pilha da causa quando há uma exceção. O registro da requisição usa o padrão da rota para omitir parâmetros e exclui corpos e mensagens de exceção.

O formato padrão é texto no terminal. Para emitir JSON, defina `LOG_FORMATO_CONSOLE=logstash` no `.env` ou no ambiente do backend.

Para ver o SQL emitido pelo Hibernate, defina `LOG_NIVEL_SQL=DEBUG` no `.env` e reinicie o backend. Para ativar em uma única execução, use `LOG_NIVEL_SQL=DEBUG ./mvnw spring-boot:run` dentro de `backend/`. O logger nativo `org.hibernate.SQL` mostra os comandos com `?` no lugar dos valores dos parâmetros. O SQL emitido durante uma requisição carrega o mesmo `requestId` do log HTTP.

## Iniciar o frontend

Em outro terminal a partir da raiz:

```bash
cd frontend
npm ci
npm run dev
```

Abra o endereço exibido pelo Vite, normalmente `http://localhost:5173`. A página inicial abre `/categorias`. O proxy de desenvolvimento encaminha `/api` para `http://localhost:8080`.

A navegação fica no botão de três pontos no canto direito do cabeçalho, com “Categorias” e, ao final, “Tema”. A opção “Tema” expande as escolhas Azul bebê e Verde e indica a seleção atual. A troca é aplicada imediatamente e salva neste navegador. O menu fecha ao acessar Categorias, clicar fora, sair dele pelo teclado ou pressionar Esc.

A interface lista as categorias e abre janelas modais para cadastrar, editar ou visualizar os detalhes. Os formulários permitem ativar/desativar categorias e exibem os erros dentro da janela; ao salvar com sucesso, a janela fecha e a lista é atualizada. As janelas podem ser fechadas pelo botão de fechar, por Cancelar ou pela tecla Esc, e ficam bloqueadas durante o salvamento. A exclusão continua exigindo confirmação na lista. Há estados de lista vazia, carregamento, sucesso, erros de validação e falhas de conexão.

### Alterar as cores

As cores da interface, incluindo as popups e os indicadores de status, ficam nas variáveis CSS de [`frontend/src/assets/theme.css`](frontend/src/assets/theme.css), importadas pelo CSS global. Há duas paletas: `azul-bebe` (padrão) e `verde` (paleta original). Cada uma tem seu bloco `:root[data-tema='...']`; o primeiro bloco `:root` reúne as cores compartilhadas. O azul bebê também serve de fallback quando o atributo está ausente. Algumas variáveis usadas pelo tema azul bebê:

```css
:root {
  --cor-superficie: #fff; /* Cabeçalho, painéis, campos e popups */
}

:root[data-tema='azul-bebe'] {
  --cor-primaria: #b3d9f2; /* Botões principais e checkbox */
  --cor-primaria-hover: #9ccbed; /* Botão principal ao passar o mouse */
  --cor-texto-sobre-destaque: #1e405b; /* Texto dos botões principais */
  --cor-link: #2b638b; /* Navegação e ações em texto */
  --cor-fundo: #f0f7fd; /* Fundo da aplicação */
  --cor-texto: #243e52; /* Texto principal */
}
```

As cores de sucesso, erro e status têm variáveis próprias. Ao mudar o tema, ajuste também as cores de hover e foco e mantenha contraste suficiente entre textos e fundos. Com `npm run dev`, basta salvar o arquivo para ver as alterações no navegador, sem reiniciar os serviços. Em produção, gere e publique um novo build do frontend.

### Selecionar o tema pela interface

Use o botão de três pontos do cabeçalho, abra “Tema” e escolha Azul bebê ou Verde. O tema inicial é azul bebê quando ainda não existe uma preferência válida salva. Se o armazenamento do navegador estiver indisponível, a seleção continua funcionando durante o uso da página.

[`frontend/src/theme.ts`](frontend/src/theme.ts) centraliza `temasDisponiveis`, o tipo `Tema`, o `temaPadrao`, o estado reativo `temaAtual` e as funções `inicializarTema` e `aplicarTema`. Antes de montar a aplicação, o frontend restaura a preferência salva no `localStorage`, na chave `financas-pessoais:tema`. Outros componentes também podem chamar:

```ts
import { aplicarTema } from '@/theme'

aplicarTema('verde')
aplicarTema('azul-bebe')
```

A função altera `data-tema` no elemento `html`, atualiza `temaAtual` e salva a preferência. O CSS atualiza a página e as popups imediatamente, preservando os dados dos formulários. Use `aplicarTema` para manter o tema, o seletor e a preferência sincronizados.

### Textos e tradução

O idioma atual é português do Brasil (`pt-BR`). Os textos ficam em catálogos separados para Vue I18n e Spring MessageSource; não há seletor de idioma nesta versão.

No frontend, edite [`frontend/src/i18n/locales/pt-BR.json`](frontend/src/i18n/locales/pt-BR.json). Organize chaves semânticas por funcionalidade e inclua mensagens, botões, placeholders, nomes de temas e rótulos de acessibilidade. Nos componentes com Composition API:

```ts
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
// No template: {{ t('categorias.titulo') }}
// Com parâmetros: {{ t('categorias.avisoExclusao', { nome: categoria.nome }) }}
```

Fora dos componentes, como nos serviços HTTP, importe `i18n` de `@/i18n` e use `i18n.global.t('erros.conexao')`. Evite concatenar fragmentos de frases. Os metadados `app.titulo` e `app.descricao` também vêm do catálogo: o Vite os inclui no HTML inicial e `inicializarIdioma` sincroniza título, descrição e `html.lang` no navegador. Ordenações usam o locale da interface.

No backend, edite [`backend/src/main/resources/messages.properties`](backend/src/main/resources/messages.properties), em UTF-8. O `MessageSource` do Spring resolve títulos e detalhes no `ApiExceptionHandler`, usando o locale da requisição. Exceções de negócio carregam apenas os dados necessários à apresentação. Bean Validation referencia as mesmas mensagens por chave:

```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CategoriaRequest(
    @NotBlank(message = "{categoria.nome.obrigatorio}")
    @Size(max = 100, message = "{categoria.nome.tamanho}")
    String nome
) {}
```

Em mensagens do `MessageSource`, os parâmetros são posicionais (`{0}`, `{1}`); Bean Validation usa parâmetros da restrição, como `{max}`. No Vue I18n, use parâmetros nomeados, como `{nome}`. Nomes e descrições cadastrados pelos usuários são dados e permanecem como foram informados.

As chamadas HTTP enviam `Accept-Language` com o locale atual. [`IdiomaConfiguration`](backend/src/main/java/br/com/financas/backend/common/IdiomaConfiguration.java) define os idiomas aceitos e o padrão `pt-BR`, usado também quando o idioma solicitado ainda não é suportado. O catálogo base em português não depende do locale do sistema operacional. Para adicionar um idioma, crie seu JSON no frontend, registre-o em `src/i18n.ts`, acrescente o bundle correspondente no backend (por exemplo, `messages_en_US.properties`) e registre o locale no resolver. Uma futura seleção de idioma deve atualizar `i18n.global.locale.value`.

Os erros da API incluem um `codigo` estável para tratamento pelo cliente; não compare frases traduzidas. O frontend apresenta `erros` por campo ou `detail` retornados pela API e usa traduções locais para falhas de conexão e respostas sem uma mensagem válida. Exemplo:

```json
{
  "type": "about:blank",
  "title": "Nome já utilizado",
  "status": 409,
  "detail": "Já existe uma categoria com esse nome.",
  "codigo": "CATEGORIA_NOME_DUPLICADO"
}
```

## API de categorias

| Método | Endpoint | Resposta de sucesso |
| --- | --- | --- |
| GET | `/api/categorias` | `200`, lista de categorias, inclusive inativas |
| GET | `/api/categorias/{id}` | `200`, categoria |
| POST | `/api/categorias` | `201`, categoria criada e cabeçalho `Location` |
| PUT | `/api/categorias/{id}` | `200`, categoria atualizada |
| DELETE | `/api/categorias/{id}` | `204`, sem corpo |

Corpo de criação e atualização:

```json
{
  "nome": "Alimentação",
  "descricao": "Mercado e refeições",
  "ativa": true
}
```

- `id` é gerado pelo banco e não deve ser enviado.
- `nome` é obrigatório, tem no máximo 100 caracteres e recebe remoção de espaços nas extremidades. Nomes são únicos sem distinguir maiúsculas/minúsculas: `Mercado` e `mercado` conflitam, inclusive se a categoria estiver inativa.
- `descricao` é opcional, tem no máximo 500 caracteres e fica `null` quando vazia.
- `ativa` tem padrão `true` quando omitida ou `null`, tanto no POST quanto no PUT. Envie `false` explicitamente para manter uma categoria inativa em uma atualização.
- PUT substitui os campos da categoria; DELETE faz exclusão definitiva.
- Entradas inválidas retornam `400`, categorias inexistentes `404` e nomes duplicados `409`. Erros usam Problem Details com `title` e `detail` traduzidos, `codigo` estável e `erros` por campo quando aplicável. Os códigos atuais são `DADOS_INVALIDOS`, `REQUISICAO_INVALIDA`, `CATEGORIA_NAO_ENCONTRADA`, `CATEGORIA_NOME_DUPLICADO`, `CONFLITO_DADOS` e `ERRO_INTERNO` para falhas inesperadas (`500`).

Exemplo:

```bash
curl -i -X POST http://localhost:8080/api/categorias \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Alimentação","descricao":"Mercado e refeições"}'
```

## Testes e builds

Backend, a partir de `backend/`:

```bash
./mvnw test
./mvnw verify
```

`verify` executa os testes e gera o JAR executável em `target/backend-0.0.1-SNAPSHOT.jar`.

Os testes de service verificam as regras de negócio; os de controller verificam HTTP, validação e erros. `CategoriaIntegrationTest` inicia PostgreSQL 18 descartável via Testcontainers, executa a migration e verifica o CRUD completo, unicidade no banco e valor padrão de `ativa`. Não utiliza o banco de desenvolvimento e não exige `docker compose up`.

Sem acesso ao Docker, os três testes de integração são marcados como **pulados**, enquanto os demais continuam executando. Para executar apenas a integração com Docker disponível:

```bash
./mvnw -Dtest=CategoriaIntegrationTest test
```

Frontend, a partir de `frontend/`:

```bash
npm run test:unit -- --run
npm run build
npm run lint
```

Os testes usam Vitest e Vue Test Utils, com HTTP simulado, cobrindo cadastro, edição, exclusão confirmada, carregamento e falhas. O build verifica TypeScript e gera arquivos em `dist/`. O script de lint existente aplica correções automáticas.

O proxy Vite é uma configuração de desenvolvimento. Ao hospedar `dist/`, configure o servidor para encaminhar `/api` ao backend e servir `index.html` para rotas do Vue Router, como `/categorias`.

Se aparecer `Non-resolvable parent POM` junto de `maven.logusinfo.com.br: Name or service not known`, o Maven está tentando acessar um espelho corporativo cujo domínio não foi encontrado. Confirme que o comando foi executado dentro de `backend/`, usando a configuração `.mvn/` deste projeto. No IntelliJ, caso a importação Maven ignore `maven.config`, selecione `backend/.mvn/settings.xml` como arquivo de configurações do Maven deste projeto e recarregue-o.
