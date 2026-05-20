<template>
  <div class="login-page">
    <aside class="login-sidebar" aria-label="LifeOS highlights">
      <div class="login-sidebar__rings" aria-hidden="true">
        <span class="login-sidebar__ring" />
        <span class="login-sidebar__ring login-sidebar__ring--2" />
        <span class="login-sidebar__ring login-sidebar__ring--3" />
      </div>

      <div class="login-sidebar__inner">
        <header class="login-brand">
          <span class="login-brand__mark" aria-hidden="true" />
          <span class="login-brand__name">LifeOS</span>
        </header>

        <div class="login-sidebar__hero">
          <h1 class="login-sidebar__heading">Learn by doing, not watching.</h1>
          <p class="login-sidebar__lead">
            Replace passive studying with active, interactive learning. Built for better
            understanding, not just text boring.
          </p>
        </div>

        <ul class="login-features">
          <li class="login-feature">
            <span class="login-feature__icon" aria-hidden="true" />
            <div class="login-feature__text">
              <span class="login-feature__title">Interactive Learning</span>
              <span class="login-feature__desc">Adjust variables and see results update live.</span>
            </div>
          </li>
          <li class="login-feature">
            <span class="login-feature__icon" aria-hidden="true" />
            <div class="login-feature__text">
              <span class="login-feature__title">Flashcard Recalling</span>
              <span class="login-feature__desc">Beat the short-term memories.</span>
            </div>
          </li>
          <li class="login-feature">
            <span class="login-feature__icon" aria-hidden="true" />
            <div class="login-feature__text">
              <span class="login-feature__title">Gamified System</span>
              <span class="login-feature__desc">Earn XP, achievement badges, and learning streak.</span>
            </div>
          </li>
          <li class="login-feature">
            <span class="login-feature__icon" aria-hidden="true" />
            <div class="login-feature__text">
              <span class="login-feature__title">AI Learning Assistant</span>
              <span class="login-feature__desc">On‑demand help without leaving the page.</span>
            </div>
          </li>
        </ul>
      </div>
    </aside>

    <main class="login-main">
      <div class="login-card">
        <header class="login-card__header">
          <h2 class="login-card__title">Welcome back</h2>
          <p class="login-card__subtitle">Sign in to continue your learning journey</p>
        </header>

        <form class="login-form" @submit.prevent="handleLogin">
          <p v-if="formError" class="login-alert" role="alert">{{ formError }}</p>

          <div class="login-field">
            <label class="login-label" for="login-email">Email</label>
            <input
              id="login-email"
              v-model="email"
              class="login-input"
              type="email"
              name="email"
              autocomplete="email"
              placeholder=""
              required
            />
          </div>

          <div class="login-field">
            <label class="login-label" for="login-password">Password</label>
            <input
              id="login-password"
              v-model="password"
              class="login-input"
              type="password"
              name="password"
              autocomplete="current-password"
              placeholder=""
              required
            />
          </div>

          <div class="login-row">
            <RouterLink class="login-link login-link--solo" to="/forgot-password">
              Forgot password?
            </RouterLink>
          </div>

          <button class="login-submit" type="submit" :disabled="isSubmitting">
            {{ isSubmitting ? 'Logging in...' : 'Login' }}
          </button>
        </form>

        <p class="login-footer">
          New user?
          <RouterLink class="login-link" to="/register">Sign Up</RouterLink>
        </p>
      </div>
    </main>
  </div>
</template>

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
    const data = await login({
      email: email.value.trim(),
      password: password.value
    })
    if (data.user.role === 'ROLE_ADMIN') {
      await router.push('/courses')
    } else if (data.user.role === 'ROLE_INSTRUCTOR') {
      await router.push('/teacher/courses')
    } else {
      await router.push('/')
    }
  } catch (error) {
    formError.value = getErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  /* tuned to match the reference screenshot */
  --login-navy: #1c1b47;
  --login-accent: #4f4ee8;
  --login-accent-2: #4b4be1;
  --login-input-bg: #f3f4f9;
  --login-input-border: #cfd5e3;
  --login-muted: #9aa1ae;
  --login-form-text: #111827;

  min-height: 100svh;
  display: flex;
  flex-direction: column;
  width: 100%;
  margin: 0;
  font-family: 'Inter', system-ui, 'Segoe UI', Roboto, sans-serif;
  color: var(--login-form-text);
  background: #fff;
}

.login-sidebar {
  position: relative;
  flex: 1 1 50%;
  min-height: 280px;
  background: var(--login-navy);
  color: #fff;
  overflow: hidden;
  display: flex;
  align-items: stretch;
}

.login-sidebar__rings {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.login-sidebar__ring {
  position: absolute;
  right: -210px;
  top: -120px;
  width: 760px;
  height: 760px;
  border-radius: 50%;
  border: 1px solid rgba(156, 163, 255, 0.28);
}

.login-sidebar__ring--2 {
  width: 980px;
  height: 980px;
  right: -330px;
  top: -220px;
  border-color: rgba(156, 163, 255, 0.18);
}

.login-sidebar__ring--3 {
  width: 1220px;
  height: 1220px;
  right: -420px;
  top: -320px;
  border-color: rgba(156, 163, 255, 0.12);
}

.login-sidebar__inner {
  position: relative;
  z-index: 1;
  padding: clamp(32px, 5vw, 64px);
  display: flex;
  flex-direction: column;
  gap: 36px;
  justify-content: flex-start;
  max-width: 560px;
  margin-inline: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
}

.login-brand__mark {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: #4f4ee8;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.22);
  flex-shrink: 0;
}

.login-brand__name {
  font-family: 'Playfair Display', Georgia, 'Times New Roman', serif;
  font-size: 1.35rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #fff;
}

.login-sidebar__heading {
  font-family: 'Playfair Display', Georgia, 'Times New Roman', serif;
  font-size: clamp(2.6rem, 3.9vw, 3.35rem);
  font-weight: 600;
  line-height: 1.02;
  margin: 0;
  color: #fff;
  letter-spacing: -0.02em;
}

.login-sidebar__lead {
  margin: 14px 0 0;
  font-size: 1.02rem;
  line-height: 1.6;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.74);
  max-width: 44ch;
}

.login-features {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 26px;
}

.login-feature {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.login-feature__icon {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  flex-shrink: 0;
  background: rgba(109, 124, 255, 0.95);
  border: 0;
}

.login-feature__text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.login-feature__title {
  font-weight: 700;
  font-size: 0.98rem;
  color: #fff;
}

.login-feature__desc {
  font-size: 0.875rem;
  line-height: 1.45;
  color: rgba(255, 255, 255, 0.62);
}

.login-main {
  flex: 1 1 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(28px, 6vw, 64px);
  background: #fff;
  box-sizing: border-box;
}

.login-card {
  width: 100%;
  max-width: 420px;
}

.login-card__header {
  margin-bottom: clamp(24px, 4vw, 32px);
}

.login-card__title {
  font-family: 'Playfair Display', Georgia, 'Times New Roman', serif;
  font-size: 2.15rem;
  font-weight: 600;
  margin: 0;
  color: #000;
  line-height: 1.2;
}

.login-card__subtitle {
  margin: 10px 0 0;
  font-size: 0.95rem;
  color: #a1a7b3;
  line-height: 1.45;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.login-alert {
  margin: 0;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #991b1b;
  font-size: 0.9rem;
  line-height: 1.4;
}

.login-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.login-label {
  font-size: 0.9rem;
  font-weight: 500;
  color: #111827;
}

.login-input {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  font-size: 1rem;
  font-family: inherit;
  border-radius: 10px;
  border: 1px solid var(--login-input-border);
  background: var(--login-input-bg);
  color: var(--login-form-text);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.login-input::placeholder {
  color: #9ca3af;
}

.login-input:hover {
  border-color: #bfc6d7;
}

.login-input:focus {
  outline: none;
  border-color: var(--login-accent);
  box-shadow: 0 0 0 3px rgba(79, 78, 232, 0.18);
}

.login-row {
  display: flex;
  justify-content: flex-end;
  margin-top: -2px;
}

.login-link {
  color: #6b5cf7;
  font-size: 0.875rem;
  font-weight: 500;
  text-decoration: none;
  transition: opacity 0.15s ease;
}

.login-link:hover {
  opacity: 0.85;
  text-decoration: underline;
}

.login-link--solo {
  margin-top: 2px;
}

.login-submit {
  margin-top: 10px;
  width: 100%;
  padding: 16px 20px;
  border: none;
  border-radius: 9999px;
  background: var(--login-accent);
  color: #fff;
  font-family: 'Playfair Display', Georgia, 'Times New Roman', serif;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(79, 78, 232, 0.28);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    background 0.15s ease;
}

.login-submit:hover {
  background: #4a49e0;
  box-shadow: 0 12px 26px rgba(79, 78, 232, 0.32);
}

.login-submit:disabled {
  cursor: not-allowed;
  opacity: 0.68;
  box-shadow: none;
}

.login-submit:active {
  transform: scale(0.99);
}

.login-submit:focus-visible {
  outline: 2px solid var(--login-accent);
  outline-offset: 3px;
}

.login-footer {
  margin: 26px 0 0;
  text-align: center;
  font-size: 0.9rem;
  color: #a1a7b3;
}

.login-footer .login-link {
  margin-left: 4px;
  font-weight: 600;
}

@media (min-width: 901px) {
  .login-page {
    flex-direction: row;
  }

  .login-sidebar {
    flex: 0 0 40%;
    min-height: 100svh;
  }

  .login-main {
    flex: 0 0 60%;
    min-height: 100svh;
  }

  .login-sidebar__inner {
    padding-top: 58px;
  }
}

@media (max-width: 900px) {
  .login-page {
    flex-direction: column;
  }

  .login-sidebar__inner {
    padding-bottom: 32px;
  }

  .login-sidebar__heading {
    max-width: 20ch;
  }

  .login-features {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  .login-main {
    flex: 1;
    align-items: flex-start;
    padding-top: 32px;
  }
}

@media (max-width: 520px) {
  .login-features {
    grid-template-columns: 1fr;
  }
}
</style>
