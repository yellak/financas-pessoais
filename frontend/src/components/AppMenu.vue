<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, useId, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { aplicarTema, temaAtual, temasDisponiveis } from '../theme'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const rota = useRoute()
const aberto = ref(false)
const temasAbertos = ref(false)
const elemento = ref<HTMLElement | null>(null)
const botao = ref<HTMLButtonElement | null>(null)
const menu = ref<HTMLElement | null>(null)
const menuId = useId()
const temasId = useId()
const opcoesTema = ref<HTMLFieldSetElement | null>(null)

function fechar(devolverFoco = false) {
  aberto.value = false
  temasAbertos.value = false
  if (devolverFoco) botao.value?.focus()
}

async function alternar() {
  if (aberto.value) {
    fechar()
    return
  }
  aberto.value = true
  await nextTick()
  menu.value?.querySelector<HTMLAnchorElement>('a')?.focus()
}

async function alternarTemas() {
  temasAbertos.value = !temasAbertos.value
  if (temasAbertos.value) {
    await nextTick()
    opcoesTema.value?.querySelector<HTMLInputElement>('input:checked')?.focus()
  }
}

function fecharAoClicarFora(evento: PointerEvent) {
  if (evento.target instanceof Node && !elemento.value?.contains(evento.target)) fechar()
}

function fecharAoSair(evento: FocusEvent) {
  // Um alvo nulo também ocorre ao clicar no texto de um label antes de ativar o radio.
  if (evento.relatedTarget instanceof Node && !elemento.value?.contains(evento.relatedTarget)) {
    fechar()
  }
}

watch(
  () => rota.fullPath,
  () => fechar(),
)
onMounted(() => document.addEventListener('pointerdown', fecharAoClicarFora))
onBeforeUnmount(() => document.removeEventListener('pointerdown', fecharAoClicarFora))
</script>

<template>
  <div
    ref="elemento"
    class="app-menu"
    @focusout="fecharAoSair"
    @keydown.esc.stop.prevent="fechar(true)"
  >
    <button
      ref="botao"
      type="button"
      class="app-menu-toggle"
      :aria-label="t('menu.principal')"
      :aria-expanded="aberto"
      :aria-controls="menuId"
      @click="alternar"
    >
      <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <circle cx="12" cy="5" r="2" />
        <circle cx="12" cy="12" r="2" />
        <circle cx="12" cy="19" r="2" />
      </svg>
    </button>
    <nav
      v-if="aberto"
      :id="menuId"
      ref="menu"
      class="app-menu-options"
      :aria-label="t('menu.navegacao')"
    >
      <ul>
        <li>
          <RouterLink to="/categorias" class="app-menu-link" @click="fechar(true)">
            {{ t('categorias.titulo') }}
          </RouterLink>
        </li>
        <li class="app-menu-theme">
          <button
            type="button"
            class="app-menu-link app-menu-theme-toggle"
            :aria-expanded="temasAbertos"
            :aria-controls="temasId"
            @click="alternarTemas"
          >
            <span>{{ t('menu.tema') }}</span>
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
              :class="{ expanded: temasAbertos }"
            >
              <path d="m6 9 6 6 6-6" />
            </svg>
          </button>
          <fieldset
            v-if="temasAbertos"
            :id="temasId"
            ref="opcoesTema"
            class="app-menu-themes"
            :aria-label="t('menu.temaAplicacao')"
          >
            <label
              v-for="tema in temasDisponiveis"
              :key="tema.valor"
              class="app-menu-theme-option"
              :class="{ selected: temaAtual === tema.valor }"
            >
              <input
                type="radio"
                :name="temasId"
                :value="tema.valor"
                :checked="temaAtual === tema.valor"
                @change="aplicarTema(tema.valor)"
              />
              <span>{{ t(tema.chaveNome) }}</span>
            </label>
          </fieldset>
        </li>
      </ul>
    </nav>
  </div>
</template>

<style scoped>
.app-menu {
  display: flex;
  position: relative;
}

.app-menu-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  padding: 0;
  border: 1px solid var(--cor-borda);
  border-radius: 8px;
  background: var(--cor-superficie);
  color: var(--cor-link);
}

.app-menu-toggle:hover,
.app-menu-toggle[aria-expanded='true'] {
  background: var(--cor-superficie-hover);
}

.app-menu-options {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 20;
  width: 220px;
  max-width: calc(100vw - 40px);
  max-height: calc(100dvh - 100px);
  padding: 8px;
  border: 1px solid var(--cor-borda);
  border-radius: 10px;
  background: var(--cor-superficie);
  box-shadow: 0 12px 32px var(--cor-sombra-modal);
  overflow-y: auto;
  overscroll-behavior: contain;
}

.app-menu-options ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.app-menu-link {
  display: flex;
  align-items: center;
  min-height: 44px;
  width: 100%;
  padding: 12px 14px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--cor-link);
  font-size: 14px;
  font-weight: 650;
  text-decoration: none;
  text-align: left;
}

.app-menu-link:hover,
.app-menu-link.router-link-active {
  background: var(--cor-superficie-hover);
}

.app-menu-theme {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--cor-divisor);
}

.app-menu-theme-toggle {
  justify-content: space-between;
}

.app-menu-theme-toggle[aria-expanded='true'] {
  background: var(--cor-superficie-hover);
}

.app-menu-theme-toggle svg.expanded {
  transform: rotate(180deg);
}

.app-menu-themes {
  display: grid;
  gap: 4px;
  min-width: 0;
  margin: 4px 0 0;
  padding: 0;
  border: 0;
}

.app-menu-theme-option {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 10px 14px;
  border-radius: 6px;
  color: var(--cor-texto);
  font-size: 13px;
  cursor: pointer;
}

.app-menu-theme-option:hover,
.app-menu-theme-option.selected {
  background: var(--cor-superficie-hover);
}

.app-menu-theme-option input {
  width: 16px;
  height: 16px;
  margin: 0;
  accent-color: var(--cor-link);
}
</style>
