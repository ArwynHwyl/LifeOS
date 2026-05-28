<script setup lang="ts">
import { ref } from 'vue'
import { AxiosError } from 'axios'
import AuthPageLayout from '@/features/auth/components/AuthPageLayout.vue'
import { forgotPassword } from '@/features/auth/services/auth'

const email = ref('')
const message = ref('')
const error = ref('')
const isSubmitting = ref(false)

function getErrorMessage(errorValue: unknown) {
  if (errorValue instanceof AxiosError) {
    const data = errorValue.response?.data as { error?: string; errors?: Record<string, string> } | undefined
    if (data?.error) return data.error
    if (data?.errors) return Object.values(data.errors)[0] || 'Please enter a valid email.'
  }
  return 'Unable to send reset link. Please try again.'
}

async function handleSubmit() {
  error.value = ''
  message.value = ''
  isSubmitting.value = true
  try {
    const response = await forgotPassword(email.value.trim())
    message.value = response.message
  } catch (errorValue) {
    error.value = getErrorMessage(errorValue)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <AuthPageLayout mood="think" holding="none" bubble="Hmm, can't remember?">
    <header class="mb-5">
      <h1 class="font-display text-[30px] font-bold leading-tight tracking-tight text-lm-ink">Reset password</h1>
      <p class="mt-1.5 text-[14px] text-lm-ink-2">Enter your email and we'll send a recovery link.</p>
    </header>

    <form class="flex flex-col gap-3.5" @submit.prevent="handleSubmit">
      <div v-if="error" class="rounded-xl border-2 border-lm-red bg-lm-red-soft px-3 py-2.5 text-[12px] font-medium text-lm-red" role="alert">
        {{ error }}
      </div>
      <div v-if="message" class="rounded-xl border-2 border-lm-green bg-lm-green-soft px-3 py-2.5 text-[12px] font-medium text-lm-green">
        {{ message }}
      </div>

      <div class="flex flex-col gap-1.5">
        <label class="font-mono text-[11px] font-semibold uppercase tracking-[0.06em] text-lm-ink-3" for="forgot-email">Email</label>
        <input
          id="forgot-email"
          v-model="email"
          type="email"
          autocomplete="email"
          required
          class="w-full rounded-xl border-2 border-lm-line-soft bg-lm-bg-soft px-3.5 py-3 text-[14px] font-medium text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
        />
      </div>

      <button
        type="submit"
        class="mt-1.5 flex w-full cursor-pointer items-center justify-center gap-2 rounded-full border-2 border-lm-line bg-lm-yellow px-6 py-3 text-[16px] font-semibold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="isSubmitting"
      >
        {{ isSubmitting ? 'Sending...' : 'Send reset link' }}
        <svg v-if="!isSubmitting" class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6" /></svg>
      </button>
    </form>

    <template #footer>
      <RouterLink class="font-bold text-lm-ink" to="/login">← Back to sign in</RouterLink>
    </template>
  </AuthPageLayout>
</template>
