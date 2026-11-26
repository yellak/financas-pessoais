<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppModal from '../components/AppModal.vue'
import { categoriasApi, type Categoria, type CategoriaInput } from '../services/categorias'
import { ErroApi } from '../services/http'
import { useI18n } from 'vue-i18n'

type ModoModal = 'criar' | 'editar' | 'visualizar'

const { t, locale } = useI18n()
const router = useRouter()
const categorias = ref<Categoria[]>([])
const carregando = ref(true)
const salvando = ref(false)
const excluindoId = ref<number | null>(null)
const modoModal = ref<ModoModal | null>(null)
const categoriaSelecionada = ref<Categoria | null>(null)
const confirmarExclusaoId = ref<number | null>(null)
const erro = ref('')
const erroModal = ref('')
const sucesso = ref('')
const falhaCarregamento = ref(false)
const nomeInput = ref<HTMLInputElement | null>(null)
const formulario = reactive<CategoriaInput>({ nome: '', descricao: '', ativa: true })
const ocupado = computed(() => carregando.value || salvando.value || excluindoId.value !== null)
const ordenadas = computed(() =>
  [...categorias.value].sort((a, b) => a.nome.localeCompare(b.nome, locale.value)),
)
const tituloModal = computed(() => {
  if (modoModal.value === 'visualizar') return t('categorias.detalhes')
  return modoModal.value === 'editar' ? t('categorias.editar') : t('categorias.nova')
})

async function mensagemErro(falha: unknown) {
  if (falha instanceof ErroApi && falha.status === 401) {
    await router.replace({ name: 'login', query: { sessao: 'expirada' } })
    return ''
  }
  return falha instanceof Error ? falha.message : t('erros.inesperado')
}

async function carregar() {
  carregando.value = true
  erro.value = ''
  falhaCarregamento.value = false
  try {
    categorias.value = await categoriasApi.listar()
  } catch (falha) {
    erro.value = await mensagemErro(falha)
    falhaCarregamento.value = true
  } finally {
    carregando.value = false
  }
}

function limparFormulario() {
  Object.assign(formulario, { nome: '', descricao: '', ativa: true })
}

async function abrirModal(modo: ModoModal, categoria: Categoria | null = null) {
  if (ocupado.value) return
  limparFormulario()
  categoriaSelecionada.value = categoria
  if (categoria) {
    Object.assign(formulario, {
      nome: categoria.nome,
      descricao: categoria.descricao ?? '',
      ativa: categoria.ativa,
    })
  }
  confirmarExclusaoId.value = null
  erro.value = ''
  erroModal.value = ''
  sucesso.value = ''
  modoModal.value = modo
  await nextTick()
  if (modo !== 'visualizar') nomeInput.value?.focus()
}

function fecharModal() {
  if (salvando.value) return
  modoModal.value = null
  categoriaSelecionada.value = null
  erroModal.value = ''
  limparFormulario()
}

async function salvar() {
  if (ocupado.value || modoModal.value === null || modoModal.value === 'visualizar') return
  erroModal.value = ''
  sucesso.value = ''
  if (!formulario.nome.trim()) {
    erroModal.value = t('categorias.formulario.nomeObrigatorio')
    nomeInput.value?.focus()
    return
  }
  salvando.value = true
  const id = categoriaSelecionada.value?.id ?? null
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
      id === null ? t('categorias.sucesso.criada') : t('categorias.sucesso.atualizada')
    salvando.value = false
    fecharModal()
    confirmarExclusaoId.value = null
  } catch (falha) {
    erroModal.value = await mensagemErro(falha)
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
    confirmarExclusaoId.value = null
    sucesso.value = t('categorias.sucesso.excluida')
  } catch (falha) {
    erro.value = await mensagemErro(falha)
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
        <p class="eyebrow">{{ t('categorias.chamada') }}</p>
        <h1>{{ t('categorias.titulo') }}</h1>
        <p class="subtitle">{{ t('categorias.subtitulo') }}</p>
      </div>
      <button class="button primary" type="button" :disabled="ocupado" @click="abrirModal('criar')">
        <span aria-hidden="true">＋</span> {{ t('categorias.nova') }}
      </button>
    </div>

    <div v-if="erro" class="notice error" role="alert">{{ erro }}</div>
    <div v-if="sucesso" class="notice success" role="status">{{ sucesso }}</div>

    <section class="panel list-panel" aria-labelledby="lista-titulo" :aria-busy="carregando">
      <div class="panel-heading">
        <div class="section-title">
          <h2 id="lista-titulo">{{ t('categorias.lista') }}</h2>
          <span v-if="!carregando && !falhaCarregamento" class="count">{{
            categorias.length
          }}</span>
        </div>
        <button type="button" class="text-button" :disabled="ocupado" @click="carregar">
          {{ t('categorias.atualizarLista') }}
        </button>
      </div>

      <div v-if="carregando" class="empty-state" role="status">
        {{ t('categorias.carregando') }}
      </div>
      <div v-else-if="falhaCarregamento" class="empty-state">
        <h3>{{ t('categorias.falhaCarregamento') }}</h3>
        <p>{{ t('categorias.tentarNovamente') }}</p>
      </div>
      <div v-else-if="categorias.length === 0" class="empty-state">
        <span class="empty-icon" aria-hidden="true">＋</span>
        <h3>{{ t('categorias.listaVazia.titulo') }}</h3>
        <p>{{ t('categorias.listaVazia.descricao') }}</p>
      </div>
      <ul v-else class="category-list">
        <li v-for="categoria in ordenadas" :key="categoria.id" class="category-item">
          <div class="category-row">
            <div class="category-content">
              <h3>{{ categoria.nome }}</h3>
              <p>{{ categoria.descricao || t('categorias.semDescricao') }}</p>
            </div>
            <span class="badge" :class="{ inactive: !categoria.ativa }">{{
              categoria.ativa ? t('categorias.ativa') : t('categorias.inativa')
            }}</span>
            <div class="row-actions">
              <button
                type="button"
                class="text-button"
                :aria-label="t('categorias.visualizarNome', { nome: categoria.nome })"
                :disabled="ocupado"
                @click="abrirModal('visualizar', categoria)"
              >
                {{ t('comum.visualizar') }}
              </button>
              <button
                type="button"
                class="text-button"
                :aria-label="t('categorias.editarNome', { nome: categoria.nome })"
                :disabled="ocupado"
                @click="abrirModal('editar', categoria)"
              >
                {{ t('comum.editar') }}
              </button>
              <button
                type="button"
                class="text-button danger-text"
                :aria-label="t('categorias.excluirNome', { nome: categoria.nome })"
                :disabled="ocupado"
                @click="solicitarExclusao(categoria.id)"
              >
                {{ t('comum.excluir') }}
              </button>
            </div>
          </div>
          <div
            v-if="confirmarExclusaoId === categoria.id"
            class="delete-confirmation"
            :aria-label="t('categorias.confirmarExclusaoNome', { nome: categoria.nome })"
            role="group"
          >
            <p>{{ t('categorias.avisoExclusao', { nome: categoria.nome }) }}</p>
            <div class="form-actions">
              <button
                type="button"
                class="button danger"
                :disabled="ocupado"
                @click="excluir(categoria)"
              >
                {{
                  excluindoId === categoria.id
                    ? t('categorias.excluindo')
                    : t('categorias.confirmarExclusao')
                }}
              </button>
              <button
                type="button"
                class="button secondary"
                :disabled="ocupado"
                @click="confirmarExclusaoId = null"
              >
                {{ t('categorias.cancelarExclusao') }}
              </button>
            </div>
          </div>
        </li>
      </ul>
    </section>

    <AppModal
      v-if="modoModal !== null"
      :titulo="tituloModal"
      :ocupado="salvando"
      @fechar="fecharModal"
    >
      <div v-if="modoModal === 'visualizar' && categoriaSelecionada" class="category-details">
        <dl>
          <div>
            <dt>{{ t('categorias.nome') }}</dt>
            <dd>{{ categoriaSelecionada.nome }}</dd>
          </div>
          <div>
            <dt>{{ t('categorias.descricao') }}</dt>
            <dd class="category-description">
              {{ categoriaSelecionada.descricao || t('categorias.semDescricao') }}
            </dd>
          </div>
          <div>
            <dt>{{ t('categorias.status') }}</dt>
            <dd>
              <span class="badge" :class="{ inactive: !categoriaSelecionada.ativa }">
                {{ categoriaSelecionada.ativa ? t('categorias.ativa') : t('categorias.inativa') }}
              </span>
            </dd>
          </div>
        </dl>
        <div class="form-actions">
          <button type="button" class="button primary" @click="fecharModal">
            {{ t('comum.fechar') }}
          </button>
        </div>
      </div>
      <form v-else class="category-form" :aria-busy="salvando" @submit.prevent="salvar">
        <div v-if="erroModal" class="notice error" role="alert">{{ erroModal }}</div>
        <p class="form-hint">{{ t('categorias.formulario.orientacao') }}</p>
        <div class="field">
          <label for="nome"
            >{{ t('categorias.nome') }}
            <span class="required-note">{{ t('comum.obrigatorio') }}</span></label
          >
          <input
            id="nome"
            ref="nomeInput"
            v-model="formulario.nome"
            name="nome"
            type="text"
            :placeholder="t('categorias.formulario.nomeExemplo')"
            maxlength="100"
            required
            :disabled="ocupado"
          />
        </div>
        <div class="field">
          <label for="descricao"
            >{{ t('categorias.descricao') }}
            <span class="required-note">{{ t('comum.opcional') }}</span></label
          >
          <textarea
            id="descricao"
            v-model="formulario.descricao"
            name="descricao"
            :placeholder="t('categorias.formulario.descricaoExemplo')"
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
          <span
            >{{ t('categorias.formulario.ativa')
            }}<small>{{ t('categorias.formulario.orientacaoAtiva') }}</small></span
          >
        </label>
        <div class="form-actions">
          <button type="submit" class="button primary" :disabled="ocupado">
            {{
              salvando
                ? t('comum.salvando')
                : modoModal === 'criar'
                  ? t('categorias.formulario.cadastrar')
                  : t('comum.salvarAlteracoes')
            }}
          </button>
          <button type="button" class="button secondary" :disabled="ocupado" @click="fecharModal">
            {{ t('comum.cancelar') }}
          </button>
        </div>
      </form>
    </AppModal>
  </main>
</template>
