<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import AppMenu from './components/AppMenu.vue'
</script>

<template>
  <header class="app-header">
    <div class="header-content">
      <RouterLink to="/categorias" class="brand">Finanças pessoais</RouterLink>
      <AppMenu />
    </div>
  </header>
  <RouterView />
</template>

<style>
@import './assets/main.css';
</style>
