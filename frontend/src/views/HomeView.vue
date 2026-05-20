<template>
  <main class="home-page">
    <section class="home-panel">
      <p class="home-kicker">LifeOS</p>
      <h1>Welcome{{ username ? `, ${username}` : '' }}</h1>
      <p class="home-copy">You are signed in and ready to continue learning.</p>
      <button class="home-button" type="button" @click="logout">Log out</button>
    </section>
  </main>
</template>

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

<style scoped>
.home-page {
  min-height: 100svh;
  display: grid;
  place-items: center;
  padding: 32px;
  box-sizing: border-box;
  background: #f6f7fb;
  color: #111827;
  font-family: 'Inter', system-ui, 'Segoe UI', Roboto, sans-serif;
}

.home-panel {
  width: min(100%, 520px);
  padding: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 16px 40px rgba(17, 24, 39, 0.08);
}

.home-kicker {
  margin: 0 0 12px;
  color: #4f4ee8;
  font-weight: 700;
  font-size: 0.82rem;
  text-transform: uppercase;
}

.home-panel h1 {
  margin: 0;
  font-size: 2rem;
  line-height: 1.2;
  color: #111827;
}

.home-copy {
  margin: 12px 0 24px;
  color: #6b7280;
}

.home-button {
  border: none;
  border-radius: 9999px;
  background: #4f4ee8;
  color: #fff;
  padding: 12px 18px;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}
</style>
