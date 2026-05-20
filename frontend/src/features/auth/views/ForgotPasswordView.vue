<template>
  <main class="auth-action-page">
    <section class="auth-action-panel">
      <p class="auth-action-kicker">LifeOS</p>
      <h1>Reset password</h1>
      <p class="auth-action-copy">
        {{ message || 'Enter your email and we will send a password reset link.' }}
      </p>

      <form class="auth-action-form" @submit.prevent="handleSubmit">
        <p v-if="error" class="auth-action-alert" role="alert">{{ error }}</p>
        <input
          v-model="email"
          class="auth-action-input"
          type="email"
          autocomplete="email"
          required
        />
        <button class="auth-action-button" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? 'Sending...' : 'Send reset link' }}
        </button>
      </form>

      <RouterLink class="auth-action-link" to="/login">Back to login</RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { AxiosError } from 'axios'
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

<style scoped>
@import './auth-action.css';
</style>
