<script setup lang="ts">
import { ref } from 'vue'
import { AxiosError } from 'axios'
import { useRouter } from 'vue-router'
import AuthPageLayout from '@/features/auth/components/AuthPageLayout.vue'
import { login } from '@/features/auth/services/auth'

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const formError = ref('')
const isSubmitting = ref(false)
const router = useRouter()

function getErrorMessage(error: unknown) {
  if (error instanceof AxiosError) {
    const data = error.response?.data as { error?: string; errors?: Record<string, string> } | undefined
    if (data?.error) return data.error
    if (data?.errors) return Object.values(data.errors)[0] || 'Please check your login details.'
  }
  return 'Unable to log in. Please try again.'
}

async function handleLogin() {
  formError.value = ''
  isSubmitting.value = true
  try {
    const data = await login({ email: email.value.trim(), password: password.value })
    if (data.user.role === 'ROLE_ADMIN') {
      await router.push('/admin/courses')
    } else if (data.user.role === 'ROLE_TEACHER') {
      await router.push('/teacher/courses')
    } else {
      await router.push('/learn/courses')
    }
  } catch (error) {
    formError.value = getErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <AuthPageLayout mood="wave" holding="none" bubble="Welcome back, Learner!">
    <!-- Form card body -->
    <header class="mb-5">
      <h1 class="font-display text-[30px] font-bold leading-tight tracking-tight text-lm-ink">Sign in</h1>
      <p class="mt-1.5 text-[14px] text-lm-ink-2">Continue your learning streak.</p>
    </header>

    <form class="flex flex-col gap-3.5" @submit.prevent="handleLogin">
      <div v-if="formError" class="rounded-xl border-2 border-lm-red bg-lm-red-soft px-3 py-2.5 text-[12px] font-medium text-lm-red" role="alert">
        {{ formError }}
      </div>

      <div class="flex flex-col gap-1.5">
        <label class="font-mono text-[11px] font-semibold uppercase tracking-[0.06em] text-lm-ink-3" for="login-email">Email</label>
        <input
          id="login-email"
          v-model="email"
          type="email"
          name="email"
          autocomplete="email"
          required
          class="w-full rounded-xl border-2 border-lm-line-soft bg-lm-bg-soft px-3.5 py-3 text-[14px] font-medium text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <label class="font-mono text-[11px] font-semibold uppercase tracking-[0.06em] text-lm-ink-3" for="login-password">Password</label>
        <div class="relative">
          <input
            id="login-password"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            name="password"
            autocomplete="current-password"
            required
            class="w-full rounded-xl border-2 border-lm-line-soft bg-lm-bg-soft py-3 pl-3.5 pr-11 text-[14px] font-medium text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
          />
          <button
            type="button"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-lm-ink-3 transition hover:text-lm-ink"
            @click="showPassword = !showPassword"
          >
            <svg v-if="showPassword" class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" /><line x1="1" y1="1" x2="23" y2="23" /></svg>
            <svg v-else class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
          </button>
        </div>
      </div>

      <div class="flex justify-end">
        <RouterLink
          class="text-[12px] font-semibold text-lm-ink"
          style="border-bottom: 1.5px solid #8a8276; text-decoration: none;"
          to="/forgot-password"
        >
          Forgot password?
        </RouterLink>
      </div>

      <button
        type="submit"
        class="mt-1.5 flex w-full items-center justify-center gap-2 rounded-full border-2 border-lm-line bg-lm-yellow px-6 py-3 text-[16px] font-semibold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="isSubmitting"
      >
        {{ isSubmitting ? 'Signing in...' : 'Sign in' }}
        <svg v-if="!isSubmitting" class="h-4.5 w-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6" /></svg>
      </button>
    </form>

    <template #footer>
      New here?
      <RouterLink class="ml-1 font-bold text-lm-ink" to="/register">Create an account →</RouterLink>
    </template>
  </AuthPageLayout>
</template>
