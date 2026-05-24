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

        <h1 class="font-display text-[26px] font-bold text-lm-ink">Email verification</h1>

        <!-- Loading -->
        <div v-if="status === 'loading'" class="mt-4 flex items-center gap-2.5 text-[13px] text-lm-ink-3">
          <svg class="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 1 1-6.219-8.56" /></svg>
          {{ statusMessage }}
        </div>

        <!-- Success -->
        <div v-else-if="status === 'success'" class="mt-4 rounded-lg border-2 border-lm-green bg-lm-green-soft px-3 py-2.5 text-[12px] font-medium text-lm-green">
          {{ statusMessage }}
        </div>

        <!-- Error -->
        <div v-else class="mt-4 rounded-lg border-2 border-lm-red bg-lm-red-soft px-3 py-2.5 text-[12px] font-medium text-lm-red">
          {{ statusMessage }}
        </div>

        <div class="mt-6 border-t-2 border-lm-line-soft pt-5">
          <RouterLink class="text-[12px] font-semibold text-lm-ink underline-offset-2 hover:underline" to="/login">
            Go to login
          </RouterLink>
        </div>
      </div>
    </div>
  </main>
</template>
