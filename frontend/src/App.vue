<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import AppMenu from './components/AppMenu.vue'
import { useI18n } from 'vue-i18n'
import { sessaoApi } from '@/services/sessao'

const { t } = useI18n()
const rota = useRoute()
const router = useRouter()
const saindo = ref(false)
const erroAoSair = ref('')

async function sair() {
  if (saindo.value) return
  saindo.value = true
  erroAoSair.value = ''
  try {
    await sessaoApi.sair()
    await router.replace({ name: 'login' })
  } catch (falha) {
    erroAoSair.value = falha instanceof Error ? falha.message : t('erros.inesperado')
  } finally {
    saindo.value = false
  }
}
</script>

<template>
  <header class="app-header">
    <div class="header-content">
      <RouterLink :to="rota.name === 'login' ? '/login' : '/categorias'" class="brand">
        {{ t('app.nome') }}
      </RouterLink>
      <div v-if="rota.matched.length && rota.name !== 'login'" class="header-actions">
        <button
          v-if="rota.meta.requerSessao"
          type="button"
          class="button secondary sair"
          :disabled="saindo"
          @click="sair"
        >
          {{ t(saindo ? 'autenticacao.saindo' : 'autenticacao.sair') }}
        </button>
        <AppMenu />
      </div>
    </div>
  </header>
  <div v-if="erroAoSair && rota.meta.requerSessao" class="page erro-sessao">
    <p class="notice error" role="alert">{{ erroAoSair }}</p>
  </div>
  <RouterView />
</template>

<style>
@import './assets/main.css';

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sair {
  min-height: 44px;
}

.erro-sessao {
  padding-top: 16px;
  padding-bottom: 0;
}

.erro-sessao .notice {
  margin: 0;
}
</style>
