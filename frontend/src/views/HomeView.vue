<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = computed(() => {
  const rawUser = localStorage.getItem('authUser')
  if (!rawUser) return ''
  try {
    return (JSON.parse(rawUser) as { username?: string }).username || ''
  } catch {
    return ''
  }
})

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('authUser')
  router.push('/login')
}
</script>

<template>
  <main class="flex min-h-screen items-center justify-center bg-lm-bg p-8">
    <div class="w-full max-w-[520px] rounded-[18px] border-2 border-lm-line bg-lm-surface p-8 shadow-stamp-md">
      <p class="mb-3 font-mono text-[11px] font-bold uppercase tracking-[0.13em] text-lm-ink-3">LifeOS</p>
      <h1 class="font-display text-[32px] font-bold leading-tight text-lm-ink">
        Welcome{{ username ? `, ${username}` : '' }}
      </h1>
      <p class="mt-3 mb-6 text-[14px] leading-relaxed text-lm-ink-2">
        You are signed in and ready to continue learning.
      </p>
      <button
        type="button"
        class="rounded-full border-2 border-lm-line bg-lm-yellow px-5 py-3 text-[14px] font-bold text-lm-ink shadow-stamp-sm transition-all duration-200 hover:-translate-y-px hover:shadow-stamp-md"
        @click="logout"
      >
        Log out
      </button>
    </div>
  </main>
</template>
