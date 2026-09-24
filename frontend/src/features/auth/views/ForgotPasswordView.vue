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
  <AuthPageLayout :mood="error ? 'oops' : message ? 'happy' : 'think'" holding="none" bubble="Hmm, can't remember?">
    <header class="mb-5">
      <h1 class="m-0 font-display text-[30px] font-bold leading-tight tracking-tight text-lx-ink">Reset password</h1>
      <p class="mt-1.5 text-[14px] text-lx-ink-soft">Enter your email and we'll send a recovery link.</p>
    </header>

    <form class="flex flex-col gap-3.5" @submit.prevent="handleSubmit">
      <div v-if="error" class="auth-alert auth-alert-error" role="alert">
        {{ error }}
      </div>
      <div v-if="message" class="auth-alert auth-alert-success">
        {{ message }}
      </div>

      <div class="flex flex-col gap-1.5">
        <label class="auth-label" for="forgot-email">Email</label>
        <input
          id="forgot-email"
          v-model="email"
          type="email"
          autocomplete="email"
          required
          class="auth-input"
        />
      </div>

      <button
        type="submit"
        class="auth-btn"
        :disabled="isSubmitting"
      >
        {{ isSubmitting ? 'Sending...' : 'Send reset link' }}
        <svg v-if="!isSubmitting" class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6" /></svg>
      </button>
    </form>

    <template #footer>
      <RouterLink class="auth-link" to="/login">← Back to sign in</RouterLink>
    </template>
  </AuthPageLayout>
</template>
