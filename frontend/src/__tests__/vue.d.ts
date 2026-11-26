// Compatibilidade com o serviço TypeScript quando o suporte a Vue não está ativo.
declare module '*.vue' {
  import type { DefineComponent } from 'vue'

  const componente: DefineComponent
  export default componente
}
