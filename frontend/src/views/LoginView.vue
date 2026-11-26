<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ErroApi } from '@/services/http'
import { sessaoApi } from '@/services/sessao'

const { t } = useI18n()
const rota = useRoute()
const router = useRouter()
const senhaMinima = 8
const senhaMaxima = 128
const cadastrando = ref(false)
const formulario = computed(() =>
  cadastrando.value ? 'autenticacao.cadastro' : 'autenticacao.login',
)
const enviando = ref(false)
const email = ref('')
const senha = ref('')
const campoEmail = ref<HTMLInputElement>()
const campoSenha = ref<HTMLInputElement>()
const erroEmail = ref('')
const erroSenha = ref('')
const erro = ref(rota.query.sessao === 'indisponivel' ? t('autenticacao.erros.sessao') : '')
if (rota.query.sessao === 'expirada') erro.value = t('autenticacao.erros.sessaoExpirada')
const contaCriada = ref(false)

function alterarModo() {
  cadastrando.value = !cadastrando.value
  senha.value = ''
  erro.value = ''
  erroEmail.value = ''
  erroSenha.value = ''
  contaCriada.value = false
  campoEmail.value?.focus()
}

function validarFormulario() {
  erroEmail.value = ''
  erroSenha.value = ''
  if (!email.value) {
    erroEmail.value = t('autenticacao.erros.emailObrigatorio')
  } else if (cadastrando.value && campoEmail.value?.validity.typeMismatch) {
    erroEmail.value = t('autenticacao.erros.emailInvalido')
  }

  if (!senha.value || (cadastrando.value && !senha.value.trim())) {
    erroSenha.value = t('autenticacao.erros.senhaObrigatoria')
  } else if (
    cadastrando.value &&
    (senha.value.length < senhaMinima || senha.value.length > senhaMaxima)
  ) {
    erroSenha.value = t('autenticacao.senhaTamanho', { min: senhaMinima, max: senhaMaxima })
  } else if (
    cadastrando.value &&
    (!/\p{Lu}/u.test(senha.value) ||
      !/\p{Nd}/u.test(senha.value) ||
      !/[\p{P}\p{S}]/u.test(senha.value))
  ) {
    erroSenha.value = t('autenticacao.senhaFormato')
  }

  if (erroEmail.value) campoEmail.value?.focus()
  else if (erroSenha.value) campoSenha.value?.focus()
  return !erroEmail.value && !erroSenha.value
}

async function enviar() {
  if (enviando.value) return
  erro.value = ''
  contaCriada.value = false
  if (!validarFormulario()) return

  enviando.value = true
  try {
    const credenciais = { email: email.value, senha: senha.value }
    if (cadastrando.value) {
      const usuario = await sessaoApi.cadastrar(credenciais)
      email.value = usuario.email
      senha.value = ''
      cadastrando.value = false
      contaCriada.value = true
    } else {
      await sessaoApi.entrar(credenciais)
      senha.value = ''
      await router.replace({ name: 'categorias' })
      if (rota.name === 'login') erro.value = t('autenticacao.erros.sessao')
    }
  } catch (falha) {
    if (falha instanceof ErroApi && falha.status === 401) {
      erro.value = t('autenticacao.erros.credenciais')
    } else if (falha instanceof ErroApi && falha.status === 403) {
      erro.value = t('autenticacao.erros.tentativaExpirada')
    } else {
      erro.value = falha instanceof Error ? falha.message : t('erros.inesperado')
    }
  } finally {
    enviando.value = false
  }

  if (contaCriada.value) {
    await nextTick()
    campoSenha.value?.focus()
  }
}
</script>

<template>
  <main class="page pagina-acesso" aria-labelledby="titulo-acesso">
    <section class="panel painel-acesso">
      <h1 id="titulo-acesso">{{ t(`${formulario}.titulo`) }}</h1>

      <p v-if="contaCriada" class="notice success" role="status">
        {{ t('autenticacao.cadastro.sucesso') }}
      </p>
      <p v-if="erro" class="notice error" role="alert">{{ erro }}</p>

      <form novalidate :aria-busy="enviando" @submit.prevent="enviar">
        <div class="field">
          <label for="email-acesso">{{ t('autenticacao.email') }}</label>
          <input
            id="email-acesso"
            ref="campoEmail"
            v-model.trim="email"
            name="email"
            type="email"
            autocomplete="username"
            autocapitalize="none"
            maxlength="254"
            spellcheck="false"
            required
            :disabled="enviando"
            :aria-invalid="Boolean(erroEmail)"
            :aria-describedby="erroEmail ? 'erro-email-acesso' : undefined"
            @input="erroEmail = ''"
          />
          <p v-if="erroEmail" id="erro-email-acesso" class="erro-campo" role="alert">
            {{ erroEmail }}
          </p>
        </div>

        <div class="field">
          <label for="senha-acesso">{{ t('autenticacao.senha') }}</label>
          <input
            id="senha-acesso"
            ref="campoSenha"
            v-model="senha"
            name="senha"
            type="password"
            :autocomplete="cadastrando ? 'new-password' : 'current-password'"
            required
            :disabled="enviando"
            :aria-invalid="Boolean(erroSenha)"
            :aria-describedby="
              erroSenha ? 'erro-senha-acesso' : cadastrando ? 'orientacao-senha-acesso' : undefined
            "
            @input="erroSenha = ''"
          />
          <p v-if="erroSenha" id="erro-senha-acesso" class="erro-campo" role="alert">
            {{ erroSenha }}
          </p>
          <p v-else-if="cadastrando" id="orientacao-senha-acesso" class="orientacao-campo">
            {{ t('autenticacao.senhaOrientacao', { min: senhaMinima, max: senhaMaxima }) }}
          </p>
        </div>

        <button type="submit" class="button primary enviar-acesso" :disabled="enviando">
          {{ t(`${formulario}.${enviando ? 'enviando' : 'acao'}`) }}
        </button>
      </form>

      <div class="alternar-acesso">
        <span>{{ t(`${formulario}.orientacao`) }}</span>
        <button type="button" class="text-button" :disabled="enviando" @click="alterarModo">
          {{ t(cadastrando ? 'autenticacao.login.acao' : 'autenticacao.cadastro.acao') }}
        </button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.pagina-acesso {
  width: min(100%, 470px);
  padding-top: clamp(32px, 9vh, 90px);
}

.painel-acesso {
  padding: clamp(22px, 6vw, 32px);
}

.painel-acesso h1 {
  margin-bottom: 26px;
  font-size: 28px;
}

.field input {
  min-height: 44px;
  font-size: 16px;
}

.field input[aria-invalid='true'] {
  border-color: var(--cor-perigo);
}

.orientacao-campo,
.erro-campo {
  margin: 0;
  font-size: 13px;
  line-height: 1.45;
}

.orientacao-campo {
  color: var(--cor-texto-secundario);
}

.erro-campo {
  color: var(--cor-texto-erro);
}

.enviar-acesso {
  width: 100%;
  min-height: 44px;
}

.alternar-acesso {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  column-gap: 4px;
  margin-top: 18px;
  padding-top: 12px;
  border-top: 1px solid var(--cor-divisor);
  color: var(--cor-texto-secundario);
  font-size: 13px;
}

.alternar-acesso button {
  min-height: 44px;
}
</style>
