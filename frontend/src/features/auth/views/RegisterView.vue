<script setup lang="ts">
import { computed, ref } from 'vue'
import { AxiosError } from 'axios'
import AuthPageLayout from '@/features/auth/components/AuthPageLayout.vue'
import { register } from '@/features/auth/services/auth'

const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirm = ref(false)
const passwordFocused = ref(false)
const formError = ref('')
const isSubmitting = ref(false)
const successMessage = ref('')

function getErrorMessage(error: unknown) {
  if (error instanceof AxiosError) {
    const data = error.response?.data as { error?: string; errors?: Record<string, string> } | undefined
    if (data?.error) return data.error
    if (data?.errors) return Object.values(data.errors)[0] || 'Please check your account details.'
  }
  return 'Unable to create your account. Please try again.'
}

const passwordRules = computed(() => ({
  length: password.value.length >= 8,
  cases: /[a-z]/.test(password.value) && /[A-Z]/.test(password.value),
  number: /\d/.test(password.value),
  symbol: /[^a-zA-Z0-9\s]/.test(password.value),
}))

const passwordValid = computed(() => Object.values(passwordRules.value).every(Boolean))

async function handleRegister() {
  formError.value = ''
  if (!passwordValid.value) {
    formError.value = 'Password does not meet all the requirements below.'
    return
  }
  if (password.value !== confirmPassword.value) {
    formError.value = 'Passwords do not match.'
    return
  }
  isSubmitting.value = true
  try {
    await register({ username: username.value.trim(), email: email.value.trim(), password: password.value })
    successMessage.value = 'Check your email to verify your account before signing in.'
  } catch (error) {
    formError.value = getErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <AuthPageLayout tight :mood="formError ? 'oops' : 'cheer'" :cover="passwordFocused && !(showPassword && showConfirm)" holding="pencil" bubble="Yes! Let's begin!">
    <header class="mb-4">
      <h1 class="m-0 font-display text-[28px] font-bold leading-tight tracking-tight text-lx-ink">Create account</h1>
      <p v-if="successMessage" class="mt-3 auth-alert auth-alert-success">
        {{ successMessage }}
      </p>
    </header>

    <form v-if="!successMessage" class="flex flex-col gap-3" @submit.prevent="handleRegister">
      <div v-if="formError" class="auth-alert auth-alert-error" role="alert">
        {{ formError }}
      </div>

      <div class="flex flex-col gap-1.5">
        <label class="auth-label" for="reg-username">Username</label>
        <input
          id="reg-username"
          v-model="username"
          type="text"
          name="username"
          autocomplete="username"
          required
          minlength="3"
          class="auth-input"
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <label class="auth-label" for="reg-email">Email</label>
        <input
          id="reg-email"
          v-model="email"
          type="email"
          name="email"
          autocomplete="email"
          required
          class="auth-input"
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <label class="auth-label" for="reg-password">Password</label>
        <div class="relative">
          <input
            id="reg-password"
          @focus="passwordFocused = true"
          @blur="passwordFocused = false"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            name="new-password"
            autocomplete="new-password"
            required
            minlength="8"
            class="auth-input pr-11"
          />
          <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 cursor-pointer text-lx-ink-faint transition hover:text-lx-ink" @click="showPassword = !showPassword">
            <svg v-if="showPassword" class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" /><line x1="1" y1="1" x2="23" y2="23" /></svg>
            <svg v-else class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
          </button>
        </div>
      </div>

      <!-- Password checklist -->
      <div class="auth-checklist grid grid-cols-2 gap-1.5">
        <div class="flex items-center gap-1.5" :class="passwordRules.length ? 'text-lx-feather-dark' : 'text-lx-ink-faint'">
          <svg v-if="passwordRules.length" class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M4 13l5 5L20 6" /></svg>
          <span v-else class="inline-block h-3.5 w-3.5 rounded-full border-[1.5px] border-lx-ink-faint" />
          8+ characters
        </div>
        <div class="flex items-center gap-1.5" :class="passwordRules.number ? 'text-lx-feather-dark' : 'text-lx-ink-faint'">
          <svg v-if="passwordRules.number" class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M4 13l5 5L20 6" /></svg>
          <span v-else class="inline-block h-3.5 w-3.5 rounded-full border-[1.5px] border-lx-ink-faint" />
          One number
        </div>
        <div class="flex items-center gap-1.5" :class="passwordRules.cases ? 'text-lx-feather-dark' : 'text-lx-ink-faint'">
          <svg v-if="passwordRules.cases" class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M4 13l5 5L20 6" /></svg>
          <span v-else class="inline-block h-3.5 w-3.5 rounded-full border-[1.5px] border-lx-ink-faint" />
          Upper &amp; lowercase
        </div>
        <div class="flex items-center gap-1.5" :class="passwordRules.symbol ? 'text-lx-feather-dark' : 'text-lx-ink-faint'">
          <svg v-if="passwordRules.symbol" class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M4 13l5 5L20 6" /></svg>
          <span v-else class="inline-block h-3.5 w-3.5 rounded-full border-[1.5px] border-lx-ink-faint" />
          One symbol (!@#$)
        </div>
      </div>

      <div class="flex flex-col gap-1.5">
        <label class="auth-label" for="reg-confirm">Confirm Password</label>
        <div class="relative">
          <input
            id="reg-confirm"
          @focus="passwordFocused = true"
          @blur="passwordFocused = false"
            v-model="confirmPassword"
            :type="showConfirm ? 'text' : 'password'"
            name="new-password"
            autocomplete="new-password"
            required
            minlength="8"
            class="auth-input pr-11"
          />
          <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 cursor-pointer text-lx-ink-faint transition hover:text-lx-ink" @click="showConfirm = !showConfirm">
            <svg v-if="showConfirm" class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" /><line x1="1" y1="1" x2="23" y2="23" /></svg>
            <svg v-else class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
          </button>
        </div>
      </div>

      <button
        type="submit"
        class="auth-btn"
        :disabled="isSubmitting"
      >
        {{ isSubmitting ? 'Creating account...' : 'Create account' }}
        <svg v-if="!isSubmitting" class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6" /></svg>
      </button>
    </form>

    <template #footer>
      Already on board?
      <RouterLink class="auth-link ml-1" to="/login">Sign in →</RouterLink>
    </template>
  </AuthPageLayout>
</template>
