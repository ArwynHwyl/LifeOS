<script setup lang="ts">
import { ref } from 'vue'
import { AxiosError } from 'axios'
import { register } from '@/features/auth/services/auth'

const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirm = ref(false)
const formError = ref('')
const isSubmitting = ref(false)
const successMessage = ref('')

function getErrorMessage(error: unknown) {
  if (error instanceof AxiosError) {
    const data = error.response?.data as { error?: string; errors?: Record<string, string> } | undefined
    if (data?.error) return data.error
    if (data?.errors) return Object.values(data.errors)[0] || 'Please check your account details.'
  }
  return 'Unable to create your account. Please try again.'
}

async function handleRegister() {
  formError.value = ''
  if (password.value !== confirmPassword.value) {
    formError.value = 'Passwords do not match.'
    return
  }
  isSubmitting.value = true
  try {
    await register({ username: username.value.trim(), email: email.value.trim(), password: password.value })
    successMessage.value = 'Check your email to verify your account before signing in.'
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
            Your learning<br />journey starts here.
          </h1>
          <p class="mt-4 max-w-[38ch] text-[13px] leading-relaxed text-lm-bg/60">
            Join thousands of students learning through interactive content, not passive reading.
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
            <h2 class="font-display text-[26px] font-bold text-lm-ink">Create your account</h2>
            <p v-if="successMessage" class="mt-2 rounded-lg border-2 border-lm-green bg-lm-green-soft px-3 py-2.5 text-[12px] font-medium text-lm-green">
              {{ successMessage }}
            </p>
            <p v-else class="mt-1.5 text-[13px] text-lm-ink-3">Fill in your details to get started.</p>
          </header>

          <form v-if="!successMessage" class="flex flex-col gap-4" @submit.prevent="handleRegister">
            <div v-if="formError" class="rounded-lg border-2 border-lm-red bg-lm-red-soft px-3 py-2.5 text-[12px] font-medium text-lm-red" role="alert">
              {{ formError }}
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="font-mono text-[11px] font-semibold text-lm-ink-3" for="register-username">Username</label>
              <input
                id="register-username"
                v-model="username"
                type="text"
                name="username"
                autocomplete="username"
                required
                minlength="3"
                class="w-full rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 py-2.5 text-[13px] text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
              />
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="font-mono text-[11px] font-semibold text-lm-ink-3" for="register-email">Email address</label>
              <input
                id="register-email"
                v-model="email"
                type="email"
                name="email"
                autocomplete="email"
                required
                class="w-full rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft px-3 py-2.5 text-[13px] text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
              />
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="font-mono text-[11px] font-semibold text-lm-ink-3" for="register-password">Password</label>
              <div class="relative">
                <input
                  id="register-password"
                  v-model="password"
                  :type="showPassword ? 'text' : 'password'"
                  name="new-password"
                  autocomplete="new-password"
                  required
                  minlength="8"
                  class="w-full rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft py-2.5 pl-3 pr-10 text-[13px] text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
                />
                <button
                  type="button"
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-lm-ink-3 transition hover:text-lm-ink"
                  :aria-pressed="showPassword"
                  @click="showPassword = !showPassword"
                >
                  <span class="sr-only">Toggle password visibility</span>
                  <svg v-if="showPassword" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" /><line x1="1" y1="1" x2="23" y2="23" /></svg>
                  <svg v-else class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
                </button>
              </div>
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="font-mono text-[11px] font-semibold text-lm-ink-3" for="register-confirm">Confirm Password</label>
              <div class="relative">
                <input
                  id="register-confirm"
                  v-model="confirmPassword"
                  :type="showConfirm ? 'text' : 'password'"
                  name="new-password"
                  autocomplete="new-password"
                  required
                  minlength="8"
                  class="w-full rounded-lg border-2 border-lm-line-soft bg-lm-bg-soft py-2.5 pl-3 pr-10 text-[13px] text-lm-ink outline-none transition focus:border-lm-line focus:bg-lm-surface focus:ring-2 focus:ring-lm-yellow/40"
                />
                <button
                  type="button"
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-lm-ink-3 transition hover:text-lm-ink"
                  :aria-pressed="showConfirm"
                  @click="showConfirm = !showConfirm"
                >
                  <span class="sr-only">Toggle confirm password visibility</span>
                  <svg v-if="showConfirm" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" /><line x1="1" y1="1" x2="23" y2="23" /></svg>
                  <svg v-else class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
                </button>
              </div>
            </div>

            <button
              type="submit"
              class="mt-1 w-full rounded-full border-2 border-lm-line bg-lm-yellow px-4 py-3 text-[13px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="isSubmitting"
            >
              {{ isSubmitting ? 'Creating account...' : 'Create account' }}
            </button>
          </form>

          <p class="mt-6 text-center text-[12px] text-lm-ink-3">
            Already have an account?
            <RouterLink class="ml-1 font-bold text-lm-ink underline-offset-2 hover:underline" to="/login">
              Sign in
            </RouterLink>
          </p>
        </div>
      </div>
    </main>
  </div>
</template>
