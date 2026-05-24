<script setup lang="ts">
import { ref } from 'vue'
import { AxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { login } from '@/features/auth/services/auth'

const email = ref('')
const password = ref('')
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
      await router.push('/courses')
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
  <div class="flex min-h-screen">
    <!-- Left sidebar -->
    <aside class="relative hidden lg:flex lg:w-[42%] flex-col overflow-hidden bg-lm-ink">
      <div class="absolute inset-0 bg-chalk-dots opacity-[0.07] pointer-events-none" />

      <div class="relative z-10 flex h-full flex-col gap-10 px-12 py-14">
        <!-- Brand -->
        <div class="flex items-center gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border-2 border-lm-bg/20 bg-lm-yellow font-display text-[17px] font-bold text-lm-ink shadow-stamp-sm">
            L
          </div>
          <span class="font-display text-[20px] font-bold text-lm-bg">LifeOS</span>
        </div>

        <!-- Hero -->
        <div class="mt-2">
          <h1 class="font-display text-[2.7rem] font-bold leading-[1.06] tracking-tight text-lm-bg">
            Learn by doing,<br />not watching.
          </h1>
          <p class="mt-4 max-w-[38ch] text-[13px] leading-relaxed text-lm-bg/60">
            Replace passive studying with active, interactive learning. Built for better understanding, not just memorisation.
          </p>
        </div>

        <!-- Feature list -->
        <ul class="mt-2 flex flex-col gap-5">
          <li class="flex items-start gap-3.5">
            <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-lm-bg/20 bg-lm-bg/10 text-lm-bg">
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12" /></svg>
            </div>
            <div>
              <p class="font-display text-[13px] font-bold text-lm-bg">Interactive Learning</p>
              <p class="mt-0.5 text-[12px] leading-relaxed text-lm-bg/55">Adjust variables and see results update live.</p>
            </div>
          </li>
          <li class="flex items-start gap-3.5">
            <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-lm-bg/20 bg-lm-bg/10 text-lm-bg">
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2" /><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" /></svg>
            </div>
            <div>
              <p class="font-display text-[13px] font-bold text-lm-bg">Flashcard Recalling</p>
              <p class="mt-0.5 text-[12px] leading-relaxed text-lm-bg/55">Beat short-term memory with spaced repetition.</p>
            </div>
          </li>
          <li class="flex items-start gap-3.5">
            <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-lm-bg/20 bg-lm-bg/10 text-lm-bg">
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" /></svg>
            </div>
            <div>
              <p class="font-display text-[13px] font-bold text-lm-bg">Gamified System</p>
              <p class="mt-0.5 text-[12px] leading-relaxed text-lm-bg/55">Earn XP, achievement badges, and learning streaks.</p>
            </div>
          </li>
          <li class="flex items-start gap-3.5">
            <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-lm-bg/20 bg-lm-bg/10 text-lm-bg">
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10" /><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" /><line x1="12" y1="17" x2="12.01" y2="17" /></svg>
            </div>
            <div>
              <p class="font-display text-[13px] font-bold text-lm-bg">AI Learning Assistant</p>
              <p class="mt-0.5 text-[12px] leading-relaxed text-lm-bg/55">On-demand help without leaving the page.</p>
            </div>
          </li>
        </ul>
      </div>
    </aside>

    <!-- Right panel -->
    <main class="relative flex flex-1 items-center justify-center bg-lm-bg px-8 py-12">
      <div class="absolute inset-0 bg-dot-grid opacity-30 pointer-events-none" />

      <div class="relative w-full max-w-[420px]">
        <div class="rounded-[18px] border-2 border-lm-line bg-lm-surface px-8 py-9 shadow-stamp-md">
          <!-- Mobile brand -->
          <div class="mb-6 flex items-center gap-2.5 lg:hidden">
            <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border-2 border-lm-line bg-lm-yellow font-display text-[14px] font-bold text-lm-ink shadow-stamp-sm">L</div>
            <span class="font-display text-[16px] font-bold text-lm-ink">LifeOS</span>
          </div>

          <header class="mb-7">
            <h2 class="font-display text-[26px] font-bold text-lm-ink">Welcome back</h2>
            <p class="mt-1.5 text-[13px] text-lm-ink-3">Sign in to continue your learning journey</p>
          </header>

          <form class="flex flex-col gap-4" @submit.prevent="handleLogin">
            <div v-if="formError" class="rounded-lg border-2 border-lm-red bg-lm-red-soft px-3 py-2.5 text-[12px] font-medium text-lm-red" role="alert">
              {{ formError }}
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="font-mono text-[11px] font-semibold text-lm-ink-3" for="login-email">Email</label>
              <input
                id="login-email"
                v-model="email"
                type="email"
                name="email"
                autocomplete="email"
                required
                class="w-full rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 py-2.5 text-[13px] text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
              />
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="font-mono text-[11px] font-semibold text-lm-ink-3" for="login-password">Password</label>
              <input
                id="login-password"
                v-model="password"
                type="password"
                name="password"
                autocomplete="current-password"
                required
                class="w-full rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 py-2.5 text-[13px] text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
              />
            </div>

            <div class="flex justify-end">
              <RouterLink class="text-[12px] font-semibold text-lm-ink underline-offset-2 hover:underline" to="/forgot-password">
                Forgot password?
              </RouterLink>
            </div>

            <button
              type="submit"
              class="mt-1 w-full rounded-full border-2 border-lm-line bg-lm-yellow px-4 py-3 text-[13px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="isSubmitting"
            >
              {{ isSubmitting ? 'Signing in...' : 'Sign in' }}
            </button>
          </form>

          <p class="mt-6 text-center text-[12px] text-lm-ink-3">
            New user?
            <RouterLink class="ml-1 font-bold text-lm-ink underline-offset-2 hover:underline" to="/register">
              Create account
            </RouterLink>
          </p>
        </div>
      </div>
    </main>
  </div>
</template>
