<script setup lang="ts">
import { ref } from 'vue'
import { AxiosError } from 'axios'
import { useRoute } from 'vue-router'
import { resetPassword } from '@/features/auth/services/auth'

const route = useRoute()
const password = ref('')
const confirmPassword = ref('')
const message = ref('')
const error = ref('')
const isSubmitting = ref(false)

function getToken() {
  const token = route.query.token
  return typeof token === 'string' ? token : ''
}

function getErrorMessage(errorValue: unknown) {
  if (errorValue instanceof AxiosError) {
    const data = errorValue.response?.data as { error?: string; errors?: Record<string, string> } | undefined
    if (data?.error) return data.error
    if (data?.errors) return Object.values(data.errors)[0] || 'Please check your new password.'
  }
  return 'Unable to reset password. Please try again.'
}

async function handleSubmit() {
  error.value = ''
  if (!getToken()) {
    error.value = 'Reset token is missing.'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = 'Passwords do not match.'
    return
  }
  isSubmitting.value = true
  try {
    const response = await resetPassword(getToken(), password.value)
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('authUser')
    message.value = response.message
  } catch (errorValue) {
    error.value = getErrorMessage(errorValue)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="relative flex min-h-screen items-center justify-center bg-lm-bg p-8">
    <div class="absolute inset-0 bg-dot-grid opacity-30 pointer-events-none" />

    <div class="relative w-full max-w-[460px]">
      <div class="rounded-[18px] border-2 border-lm-line bg-lm-surface px-8 py-9 shadow-stamp-md">
        <!-- Brand kicker -->
        <div class="mb-5 flex items-center gap-2.5">
          <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border-2 border-lm-line bg-lm-yellow font-display text-[14px] font-bold text-lm-ink shadow-stamp-sm">L</div>
          <span class="font-mono text-[11px] font-bold uppercase tracking-[0.13em] text-lm-ink-3">LifeOS</span>
        </div>

        <h1 class="font-display text-[26px] font-bold text-lm-ink">Choose a new password</h1>
        <p class="mt-2 text-[13px] leading-relaxed text-lm-ink-3">
          {{ message || 'Use a password with at least 8 characters.' }}
        </p>

        <form v-if="!message" class="mt-6 flex flex-col gap-4" @submit.prevent="handleSubmit">
          <div v-if="error" class="rounded-lg border-2 border-lm-red bg-lm-red-soft px-3 py-2.5 text-[12px] font-medium text-lm-red" role="alert">
            {{ error }}
          </div>

          <div class="flex flex-col gap-1.5">
            <label class="font-mono text-[11px] font-semibold text-lm-ink-3" for="reset-password">New password</label>
            <input
              id="reset-password"
              v-model="password"
              type="password"
              autocomplete="new-password"
              required
              minlength="8"
              class="w-full rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 py-2.5 text-[13px] text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <label class="font-mono text-[11px] font-semibold text-lm-ink-3" for="reset-confirm">Confirm new password</label>
            <input
              id="reset-confirm"
              v-model="confirmPassword"
              type="password"
              autocomplete="new-password"
              required
              minlength="8"
              class="w-full rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 py-2.5 text-[13px] text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
            />
          </div>

          <button
            type="submit"
            class="w-full rounded-full border-2 border-lm-line bg-lm-yellow px-4 py-3 text-[13px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="isSubmitting"
          >
            {{ isSubmitting ? 'Resetting...' : 'Reset password' }}
          </button>
        </form>

        <div class="mt-6 border-t-2 border-lm-line-soft pt-5">
          <RouterLink class="text-[12px] font-semibold text-lm-ink underline-offset-2 hover:underline" to="/login">
            ← Back to login
          </RouterLink>
        </div>
      </div>
    </div>
  </main>
</template>
