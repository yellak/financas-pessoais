<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, useId } from 'vue'

const props = withDefaults(defineProps<{ titulo: string; ocupado?: boolean }>(), {
  ocupado: false,
})
const emit = defineEmits<{ fechar: [] }>()
const janela = ref<HTMLDialogElement | null>(null)
const tituloElemento = ref<HTMLHeadingElement | null>(null)
const tituloId = useId()
let focoAnterior: HTMLElement | null = null
let overflowAnterior = ''

function solicitarFechamento() {
  if (!props.ocupado) emit('fechar')
}

onMounted(() => {
  focoAnterior = document.activeElement instanceof HTMLElement ? document.activeElement : null
  overflowAnterior = document.documentElement.style.overflow
  document.documentElement.style.overflow = 'hidden'
  janela.value?.showModal()
  tituloElemento.value?.focus()
})

onBeforeUnmount(() => {
  janela.value?.close()
  document.documentElement.style.overflow = overflowAnterior
  if (focoAnterior?.isConnected) focoAnterior.focus()
})
</script>

<template>
  <dialog
    ref="janela"
    class="modal"
    :aria-labelledby="tituloId"
    :aria-busy="ocupado"
    aria-modal="true"
    @cancel.prevent="solicitarFechamento"
  >
    <div class="panel-heading modal-heading">
      <h2 :id="tituloId" ref="tituloElemento" tabindex="-1">{{ titulo }}</h2>
      <button
        type="button"
        class="modal-close"
        aria-label="Fechar janela"
        :disabled="ocupado"
        @click="solicitarFechamento"
      >
        <span aria-hidden="true">×</span>
      </button>
    </div>
    <slot />
  </dialog>
</template>

<style scoped>
.modal {
  width: min(560px, calc(100% - 32px));
  max-height: calc(100dvh - 32px);
  padding: 0;
  border: 1px solid var(--cor-borda);
  border-radius: 12px;
  background: var(--cor-superficie);
  color: inherit;
  box-shadow: 0 24px 80px var(--cor-sombra-modal);
  overflow: auto;
  overscroll-behavior: contain;
}

.modal::backdrop {
  background: var(--cor-fundo-sobreposicao);
}

.modal-heading {
  position: sticky;
  top: 0;
  z-index: 1;
  background: var(--cor-superficie);
}

.modal-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--cor-texto-secundario);
  font-size: 26px;
}

.modal-close:hover:enabled {
  background: var(--cor-superficie-hover);
}
</style>
