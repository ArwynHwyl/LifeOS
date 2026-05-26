<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { useRoute } from 'vue-router'
import AuthPageLayout from '@/features/auth/components/AuthPageLayout.vue'
import { verifyEmail } from '@/features/auth/services/auth'

const route = useRoute()
const status = ref<'loading' | 'success' | 'error'>('loading')
const error = ref('')

const statusMessage = computed(() => {
  if (status.value === 'loading') return 'Verifying your email...'
  if (status.value === 'success') return 'Your account is ready. Time to start learning.'
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
  if (!token) { status.value = 'error'; error.value = 'Verification token is missing.'; return }
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
  <AuthPageLayout mood="cheer" holding="check" bubble="You're verified!">
    <header class="mb-5">
      <h1 class="font-display text-[30px] font-bold leading-tight tracking-tight text-lm-ink">Email verified!</h1>
      <p class="mt-1.5 text-[14px] text-lm-ink-2">{{ statusMessage }}</p>
    </header>

    <!-- Loading -->
    <div v-if="status === 'loading'" class="flex items-center gap-2.5 rounded-xl border-2 border-lm-line-soft bg-lm-bg-soft px-4 py-3 text-[13px] text-lm-ink-3">
      <svg class="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 1 1-6.219-8.56" /></svg>
      Verifying your email address...
    </div>

    <!-- Success -->
    <template v-else-if="status === 'success'">
      <!-- Success badge -->
      <div class="mb-4 flex items-center gap-3.5 rounded-xl border-2 border-lm-line bg-lm-green-soft p-4 shadow-stamp-sm">
        <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full border-2 border-lm-line bg-lm-green text-lm-bg shadow-stamp-sm">
          <svg class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M4 13l5 5L20 6" /></svg>
        </div>
        <div>
          <p class="font-display text-[16px] font-bold text-lm-ink">Email confirmed!</p>
          <p class="mt-0.5 text-[12.5px] text-lm-ink-2">Your account is active and ready.</p>
        </div>
      </div>

      <!-- Starter rewards -->
      <p class="mb-2 font-mono text-[11px] font-semibold uppercase tracking-[0.06em] text-lm-ink-3">Sign-up Rewards</p>
      <div class="mb-5 grid grid-cols-3 gap-2.5">
        <div class="rounded-xl border-2 border-lm-line bg-lm-yellow-soft py-3 text-center shadow-stamp-sm">
          <div class="text-[20px]">⚡</div>
          <div class="mt-1 font-display text-[13px] font-bold text-lm-ink">+50 XP</div>
        </div>
        <div class="rounded-xl border-2 border-lm-line bg-lm-blue-soft py-3 text-center shadow-stamp-sm">
          <div class="text-[20px]">🛡</div>
          <div class="mt-1 font-display text-[13px] font-bold text-lm-ink">×1 Shield</div>
        </div>
        <div class="rounded-xl border-2 border-lm-line bg-lm-rust-soft py-3 text-center shadow-stamp-sm">
          <div class="text-[20px]">★</div>
          <div class="mt-1 font-display text-[13px] font-bold text-lm-ink">First badge</div>
        </div>
      </div>

      <RouterLink
        to="/learn/courses"
        class="flex w-full items-center justify-center gap-2 rounded-full border-2 border-lm-line bg-lm-yellow px-6 py-3 text-[16px] font-semibold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md active:scale-[0.98]"
      >
        Start learning
        <svg class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6" /></svg>
      </RouterLink>
    </template>

    <!-- Error -->
    <div v-else class="rounded-xl border-2 border-lm-red bg-lm-red-soft px-3 py-2.5 text-[12px] font-medium text-lm-red">
      {{ error }}
    </div>

    <template #footer>
      <RouterLink class="font-bold text-lm-ink" to="/login">Go to sign in</RouterLink>
    </template>
  </AuthPageLayout>
</template>
