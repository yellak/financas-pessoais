<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { categoriasApi, type Categoria, type CategoriaInput } from '../services/categorias'

const categorias = ref<Categoria[]>([])
const carregando = ref(true)
const salvando = ref(false)
const excluindoId = ref<number | null>(null)
const editandoId = ref<number | null>(null)
const confirmarExclusaoId = ref<number | null>(null)
const erro = ref('')
const sucesso = ref('')
const falhaCarregamento = ref(false)
const nomeInput = ref<HTMLInputElement | null>(null)
const formulario = reactive<CategoriaInput>({ nome: '', descricao: '', ativa: true })
const ocupado = computed(() => carregando.value || salvando.value || excluindoId.value !== null)
const ordenadas = computed(() =>
  [...categorias.value].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')),
)

function mensagemErro(falha: unknown) {
  return falha instanceof Error ? falha.message : 'Ocorreu um erro. Tente novamente.'
}

async function carregar() {
  carregando.value = true
  erro.value = ''
  falhaCarregamento.value = false
  try {
    categorias.value = await categoriasApi.listar()
  } catch (falha) {
    erro.value = mensagemErro(falha)
    falhaCarregamento.value = true
  } finally {
    carregando.value = false
  }
}

function limparFormulario() {
  editandoId.value = null
  Object.assign(formulario, { nome: '', descricao: '', ativa: true })
}

async function novoCadastro() {
  limparFormulario()
  confirmarExclusaoId.value = null
  erro.value = ''
  sucesso.value = ''
  await nextTick()
  nomeInput.value?.focus()
}

async function editar(categoria: Categoria) {
  editandoId.value = categoria.id
  confirmarExclusaoId.value = null
  Object.assign(formulario, {
    nome: categoria.nome,
    descricao: categoria.descricao ?? '',
    ativa: categoria.ativa,
  })
  erro.value = ''
  sucesso.value = ''
  await nextTick()
  nomeInput.value?.focus()
}

async function salvar() {
  if (ocupado.value) return
  erro.value = ''
  sucesso.value = ''
  if (!formulario.nome.trim()) {
    erro.value = 'O nome é obrigatório.'
    nomeInput.value?.focus()
    return
  }
  salvando.value = true
  const id = editandoId.value
  const dados = {
    ...formulario,
    nome: formulario.nome.trim(),
    descricao: formulario.descricao?.trim() || null,
  }
  try {
    const salva =
      id === null ? await categoriasApi.criar(dados) : await categoriasApi.atualizar(id, dados)
    categorias.value = [...categorias.value.filter((categoria) => categoria.id !== salva.id), salva]
    sucesso.value =
      id === null ? 'Categoria cadastrada com sucesso.' : 'Categoria atualizada com sucesso.'
    limparFormulario()
    confirmarExclusaoId.value = null
  } catch (falha) {
    erro.value = mensagemErro(falha)
  } finally {
    salvando.value = false
  }
}

function solicitarExclusao(id: number) {
  confirmarExclusaoId.value = id
  erro.value = ''
  sucesso.value = ''
}

async function excluir(categoria: Categoria) {
  if (ocupado.value) return
  excluindoId.value = categoria.id
  erro.value = ''
  sucesso.value = ''
  try {
    await categoriasApi.excluir(categoria.id)
    categorias.value = categorias.value.filter((item) => item.id !== categoria.id)
    if (editandoId.value === categoria.id) limparFormulario()
    confirmarExclusaoId.value = null
    sucesso.value = 'Categoria excluída com sucesso.'
  } catch (falha) {
    erro.value = mensagemErro(falha)
  } finally {
    excluindoId.value = null
  }
}

onMounted(carregar)
</script>

<template>
  <main class="page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">SEU DINHEIRO, MAIS ORGANIZADO</p>
        <h1>Categorias de gastos</h1>
        <p class="subtitle">Defina as categorias que fazem sentido para o seu dia a dia.</p>
      </div>
      <button class="button primary" type="button" :disabled="ocupado" @click="novoCadastro">
        <span aria-hidden="true">＋</span> Nova categoria
      </button>
    </div>

    <div v-if="erro" class="notice error" role="alert">{{ erro }}</div>
    <div v-if="sucesso" class="notice success" role="status">{{ sucesso }}</div>

    <div class="workspace">
      <section class="panel list-panel" aria-labelledby="lista-titulo" :aria-busy="carregando">
        <div class="panel-heading">
          <div class="section-title">
            <h2 id="lista-titulo">Suas categorias</h2>
            <span v-if="!carregando && !falhaCarregamento" class="count">{{
              categorias.length
            }}</span>
          </div>
          <button type="button" class="text-button" :disabled="ocupado" @click="carregar">
            Atualizar lista
          </button>
        </div>

        <div v-if="carregando" class="empty-state" role="status">Carregando categorias…</div>
        <div v-else-if="falhaCarregamento" class="empty-state">
          <h3>Não foi possível carregar as categorias</h3>
          <p>Verifique a conexão e tente atualizar a lista.</p>
        </div>
        <div v-else-if="categorias.length === 0" class="empty-state">
          <span class="empty-icon" aria-hidden="true">＋</span>
          <h3>Seu primeiro passo para se organizar</h3>
          <p>Nenhuma categoria cadastrada. Comece por alimentação, transporte ou lazer.</p>
        </div>
        <ul v-else class="category-list">
          <li v-for="categoria in ordenadas" :key="categoria.id" class="category-item">
            <div class="category-row">
              <div class="category-content">
                <h3>{{ categoria.nome }}</h3>
                <p>{{ categoria.descricao || 'Sem descrição' }}</p>
              </div>
              <span class="badge" :class="{ inactive: !categoria.ativa }">{{
                categoria.ativa ? 'Ativa' : 'Inativa'
              }}</span>
              <div class="row-actions">
                <button
                  type="button"
                  class="text-button"
                  :aria-label="`Editar ${categoria.nome}`"
                  :disabled="ocupado"
                  @click="editar(categoria)"
                >
                  Editar
                </button>
                <button
                  type="button"
                  class="text-button danger-text"
                  :aria-label="`Excluir ${categoria.nome}`"
                  :disabled="ocupado"
                  @click="solicitarExclusao(categoria.id)"
                >
                  Excluir
                </button>
              </div>
            </div>
            <div
              v-if="confirmarExclusaoId === categoria.id"
              class="delete-confirmation"
              :aria-label="`Confirmar exclusão de ${categoria.nome}`"
              role="group"
            >
              <p>Excluir “{{ categoria.nome }}”? Esta ação é definitiva.</p>
              <div class="form-actions">
                <button
                  type="button"
                  class="button danger"
                  :disabled="ocupado"
                  @click="excluir(categoria)"
                >
                  {{ excluindoId === categoria.id ? 'Excluindo…' : 'Confirmar exclusão' }}
                </button>
                <button
                  type="button"
                  class="button secondary"
                  :disabled="ocupado"
                  @click="confirmarExclusaoId = null"
                >
                  Cancelar exclusão
                </button>
              </div>
            </div>
          </li>
        </ul>
      </section>

      <section class="panel editor-panel" aria-labelledby="formulario-titulo">
        <div class="panel-heading">
          <h2 id="formulario-titulo">
            {{ editandoId === null ? 'Nova categoria' : 'Editar categoria' }}
          </h2>
        </div>
        <form class="category-form" :aria-busy="salvando" @submit.prevent="salvar">
          <p class="form-hint">Dê um nome e, se quiser, adicione uma descrição.</p>
          <div class="field">
            <label for="nome">Nome <span class="required-note">(obrigatório)</span></label>
            <input
              id="nome"
              ref="nomeInput"
              v-model="formulario.nome"
              name="nome"
              type="text"
              placeholder="Ex.: Alimentação"
              maxlength="100"
              required
              :disabled="ocupado"
            />
          </div>
          <div class="field">
            <label for="descricao">Descrição <span class="required-note">(opcional)</span></label>
            <textarea
              id="descricao"
              v-model="formulario.descricao"
              name="descricao"
              placeholder="O que você vai incluir nesta categoria?"
              maxlength="500"
              rows="4"
              :disabled="ocupado"
            />
          </div>
          <label class="checkbox-field" for="ativa">
            <input
              id="ativa"
              v-model="formulario.ativa"
              name="ativa"
              type="checkbox"
              :disabled="ocupado"
            />
            <span>Categoria ativa<small>Desmarque para manter a categoria inativa.</small></span>
          </label>
          <div class="form-actions">
            <button type="submit" class="button primary" :disabled="ocupado">
              {{
                salvando
                  ? 'Salvando…'
                  : editandoId === null
                    ? 'Cadastrar categoria'
                    : 'Salvar alterações'
              }}
            </button>
            <button
              v-if="editandoId !== null"
              type="button"
              class="button secondary"
              :disabled="ocupado"
              @click="novoCadastro"
            >
              Cancelar edição
            </button>
          </div>
        </form>
      </section>
    </div>
  </main>
</template>
