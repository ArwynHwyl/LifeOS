<script setup lang="ts">
import { computed, ref } from 'vue'
import { AxiosError } from 'axios'
import { useRoute } from 'vue-router'
import AuthPageLayout from '@/features/auth/components/AuthPageLayout.vue'
import { resetPassword } from '@/features/auth/services/auth'

const route = useRoute()
const password = ref('')
const confirmPassword = ref('')
const message = ref('')
const error = ref('')
const isSubmitting = ref(false)

const strengthLevel = computed(() => {
  const p = password.value
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score++
  if (/\d/.test(p)) score++
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) score++
  if (/[^a-zA-Z0-9]/.test(p)) score++
  return score
})

const strengthLabel = computed(() => ['', 'Weak', 'Fair', 'Good', 'Strong'][strengthLevel.value] ?? '')
const strengthColor = computed(() => ['', 'text-lm-red', 'text-lm-rust', 'text-lm-yellow', 'text-lm-green'][strengthLevel.value] ?? '')
const barColor = (i: number) => {
  if (i > strengthLevel.value) return 'bg-lm-bg-soft border-lm-line-soft'
  const c = ['', 'bg-lm-red', 'bg-lm-rust', 'bg-lm-yellow', 'bg-lm-green']
  return (c[strengthLevel.value] ?? 'bg-lm-bg-soft') + ' border-lm-line'
}

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
  if (!getToken()) { error.value = 'Reset token is missing.'; return }
  if (strengthLevel.value < 4) { error.value = 'Password does not meet all the requirements below.'; return }
  if (password.value !== confirmPassword.value) { error.value = 'Passwords do not match.'; return }
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
  <AuthPageLayout medium tight mood="happy" holding="key" bubble="Got your shiny new key!">
    <header class="mb-4">
      <h1 class="font-display text-[28px] font-bold leading-tight tracking-tight text-lm-ink">Choose a new password</h1>
      <p v-if="message" class="mt-2 rounded-xl border-2 border-lm-green bg-lm-green-soft px-3 py-2.5 text-[12px] font-medium text-lm-green">{{ message }}</p>
      <p v-else class="mt-1.5 text-[14px] text-lm-ink-2">Make it strong. You won't need it again for a while.</p>
    </header>

    <form v-if="!message" class="flex flex-col gap-2.5" @submit.prevent="handleSubmit">
      <div v-if="error" class="rounded-xl border-2 border-lm-red bg-lm-red-soft px-3 py-2.5 text-[12px] font-medium text-lm-red" role="alert">
        {{ error }}
      </div>

      <!-- New password with strength meter -->
      <div class="flex flex-col gap-1.5">
        <label class="font-mono text-[11px] font-semibold uppercase tracking-[0.06em] text-lm-ink-3" for="reset-password">New Password</label>
        <input
          id="reset-password"
          v-model="password"
          type="password"
          autocomplete="new-password"
          required
          minlength="8"
          class="w-full rounded-xl border-2 border-lm-line-soft bg-lm-bg-soft px-3.5 py-3 text-[14px] font-medium text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
        />
        <!-- Strength bar -->
        <div class="flex gap-1.5">
          <div v-for="i in 4" :key="i" class="h-1.5 flex-1 rounded-full border-[1.5px] transition-all duration-200" :class="barColor(i)" />
        </div>
        <div v-if="password" class="flex justify-between text-[11px]">
          <span>Strength: <b :class="strengthColor">{{ strengthLabel }}</b></span>
          <span class="font-mono text-lm-ink-3">{{ password.length }} / 8+ chars</span>
        </div>
      </div>

      <!-- Checklist -->
      <div class="grid grid-cols-2 gap-1 rounded-xl border-2 border-dashed border-lm-line bg-lm-bg-soft p-2.5 text-[12px]">
        <div class="flex items-center gap-1.5" :class="password.length >= 8 ? 'text-lm-green' : 'text-lm-ink-3'">
          <svg v-if="password.length >= 8" class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M4 13l5 5L20 6" /></svg>
          <span v-else class="inline-block h-3.5 w-3.5 rounded-full border-[1.5px] border-lm-ink-3" />
          8+ characters
        </div>
        <div class="flex items-center gap-1.5" :class="/\d/.test(password) ? 'text-lm-green' : 'text-lm-ink-3'">
          <svg v-if="/\d/.test(password)" class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M4 13l5 5L20 6" /></svg>
          <span v-else class="inline-block h-3.5 w-3.5 rounded-full border-[1.5px] border-lm-ink-3" />
          One number
        </div>
        <div class="flex items-center gap-1.5" :class="/[a-z]/.test(password) && /[A-Z]/.test(password) ? 'text-lm-green' : 'text-lm-ink-3'">
          <svg v-if="/[a-z]/.test(password) && /[A-Z]/.test(password)" class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M4 13l5 5L20 6" /></svg>
          <span v-else class="inline-block h-3.5 w-3.5 rounded-full border-[1.5px] border-lm-ink-3" />
          Upper &amp; lowercase
        </div>
        <div class="flex items-center gap-1.5" :class="/[^a-zA-Z0-9]/.test(password) ? 'text-lm-green' : 'text-lm-ink-3'">
          <svg v-if="/[^a-zA-Z0-9]/.test(password)" class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M4 13l5 5L20 6" /></svg>
          <span v-else class="inline-block h-3.5 w-3.5 rounded-full border-[1.5px] border-lm-ink-3" />
          One symbol (!@#$)
        </div>
      </div>

      <div class="flex flex-col gap-1.5">
        <label class="font-mono text-[11px] font-semibold uppercase tracking-[0.06em] text-lm-ink-3" for="reset-confirm">Confirm Password</label>
        <input
          id="reset-confirm"
          v-model="confirmPassword"
          type="password"
          autocomplete="new-password"
          required
          minlength="8"
          class="w-full rounded-xl border-2 border-lm-line-soft bg-lm-bg-soft px-3.5 py-3 text-[14px] font-medium text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
        />
      </div>

      <button
        type="submit"
        class="flex w-full cursor-pointer items-center justify-center gap-2 rounded-full border-2 border-lm-line bg-lm-yellow px-6 py-3 text-[16px] font-semibold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="isSubmitting"
      >
        {{ isSubmitting ? 'Resetting...' : 'Reset password' }}
        <svg v-if="!isSubmitting" class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6" /></svg>
      </button>
    </form>

    <template #footer>
      <RouterLink class="font-bold text-lm-ink" to="/login">← Back to sign in</RouterLink>
    </template>
  </AuthPageLayout>
</template>
