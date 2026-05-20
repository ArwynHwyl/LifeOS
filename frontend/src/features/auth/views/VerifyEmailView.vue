<template>
  <main class="auth-action-page">
    <section class="auth-action-panel">
      <p class="auth-action-kicker">LifeOS</p>
      <h1>Email verification</h1>
      <p class="auth-action-copy">{{ statusMessage }}</p>
      <RouterLink class="auth-action-link" to="/login">Go to login</RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { useRoute } from 'vue-router'
import { verifyEmail } from '@/features/auth/services/auth'

const route = useRoute()
const status = ref<'loading' | 'success' | 'error'>('loading')
const error = ref('')

const statusMessage = computed(() => {
  if (status.value === 'loading') return 'Verifying your email...'
  if (status.value === 'success') return 'Your email has been verified. You can now sign in.'
  return error.value
})

function getToken() {
  const token = route.query.token
  return typeof token === 'string' ? token : ''
}

function getErrorMessage(errorValue: unknown) {
  if (errorValue instanceof AxiosError) {
    const data = errorValue.response?.data as { error?: string; errors?: Record<string, string> } | undefined
    if (data?.error) return data.error
    if (data?.errors) return Object.values(data.errors)[0] || 'Verification failed.'
  }
  return 'Verification failed.'
}

onMounted(async () => {
  const token = getToken()
  if (!token) {
    status.value = 'error'
    error.value = 'Verification token is missing.'
    return
  }

  try {
    await verifyEmail(token)
    status.value = 'success'
  } catch (errorValue) {
    status.value = 'error'
    error.value = getErrorMessage(errorValue)
  }
})
</script>

<style scoped>
@import './auth-action.css';
</style>
