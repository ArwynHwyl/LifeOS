<template>
  <main class="auth-action-page">
    <section class="auth-action-panel">
      <p class="auth-action-kicker">LifeOS</p>
      <h1>Choose a new password</h1>
      <p class="auth-action-copy">
        {{ message || 'Use a password with at least 8 characters.' }}
      </p>

      <form v-if="!message" class="auth-action-form" @submit.prevent="handleSubmit">
        <p v-if="error" class="auth-action-alert" role="alert">{{ error }}</p>
        <input
          v-model="password"
          class="auth-action-input"
          type="password"
          autocomplete="new-password"
          required
          minlength="8"
        />
        <input
          v-model="confirmPassword"
          class="auth-action-input"
          type="password"
          autocomplete="new-password"
          required
          minlength="8"
        />
        <button class="auth-action-button" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? 'Resetting...' : 'Reset password' }}
        </button>
      </form>

      <RouterLink class="auth-action-link" to="/login">Back to login</RouterLink>
    </section>
  </main>
</template>

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

<style scoped>
@import './auth-action.css';
</style>
