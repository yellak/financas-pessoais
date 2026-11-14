# Frontend de finanças pessoais

Vue 3, TypeScript e Vite. Os comandos de instalação, execução, testes e configuração da API estão no [README do projeto](../README.md).

```bash
npm ci
npm run dev
npm run test:unit -- --run
npm run build
npm run lint
```

A página `/categorias` consome `/api/categorias`. Durante o desenvolvimento, o Vite encaminha `/api` para `http://localhost:8080`.
