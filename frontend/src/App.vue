<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header class="app-header">
    <div class="header-content">
      <RouterLink to="/categorias" class="brand">Finanças pessoais</RouterLink>
      <nav aria-label="Navegação principal">
        <RouterLink to="/categorias">Categorias</RouterLink>
      </nav>
    </div>
  </header>
  <RouterView />
</template>

<style>
@import './assets/main.css';
</style>
