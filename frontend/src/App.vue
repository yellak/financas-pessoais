<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import AppMenu from './components/AppMenu.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <header class="app-header">
    <div class="header-content">
      <RouterLink to="/categorias" class="brand">{{ t('app.nome') }}</RouterLink>
      <AppMenu />
    </div>
  </header>
  <RouterView />
</template>

<style>
@import './assets/main.css';
</style>
