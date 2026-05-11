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
          <h2 class="login-card__title">Create your account</h2>
          <p class="login-card__subtitle">Fill in your details to get started.</p>
        </header>

        <form class="login-form" @submit.prevent>
          <div class="login-field">
            <label class="login-label" for="register-username">Username</label>
            <input
              id="register-username"
              v-model="username"
              class="login-input"
              type="text"
              name="username"
              autocomplete="username"
              placeholder=""
            />
          </div>

          <div class="login-field">
            <label class="login-label" for="register-email">Email address</label>
            <input
              id="register-email"
              v-model="email"
              class="login-input"
              type="email"
              name="email"
              autocomplete="email"
              placeholder=""
            />
          </div>

          <div class="login-field login-field--with-icon">
            <label class="login-label" for="register-password">Password</label>
            <div class="login-input-wrapper">
              <input
                id="register-password"
                v-model="password"
                class="login-input login-input--with-icon"
                :type="showPassword ? 'text' : 'password'"
                name="new-password"
                autocomplete="new-password"
                placeholder=""
              />
              <button
                type="button"
                class="input-eye"
                @click="showPassword = !showPassword"
                :aria-pressed="showPassword"
              >
                <span class="sr-only">Toggle password visibility</span>
                👁
              </button>
            </div>
          </div>

          <div class="login-field login-field--with-icon">
            <label class="login-label" for="register-confirm">Confirm Password</label>
            <div class="login-input-wrapper">
              <input
                id="register-confirm"
                v-model="confirmPassword"
                class="login-input login-input--with-icon"
                :type="showConfirm ? 'text' : 'password'"
                name="new-password"
                autocomplete="new-password"
                placeholder=""
              />
              <button
                type="button"
                class="input-eye"
                @click="showConfirm = !showConfirm"
                :aria-pressed="showConfirm"
              >
                <span class="sr-only">Toggle confirm password visibility</span>
                👁
              </button>
            </div>
          </div>

          <button class="login-submit" type="submit">Create Account</button>
        </form>

        <p class="login-footer">
          Already have an account?
          <RouterLink class="login-link" to="/login">Sign In</RouterLink>
        </p>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirm = ref(false)
</script>

<style scoped>
.login-page {
  /* same base styles as LoginView */
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
  font-size: 2.05rem;
  font-weight: 600;
  margin: 0;
  color: #000;
  line-height: 1.2;
}

.login-card__subtitle {
  margin: 8px 0 0;
  font-size: 0.95rem;
  color: #a1a7b3;
  line-height: 1.45;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
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

/* register-only tweaks */
.login-field--with-icon .login-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.login-input--with-icon {
  padding-right: 42px;
}

.input-eye {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  font-size: 1.1rem;
  line-height: 1;
  color: #9aa1ae;
}

.input-eye:hover {
  color: #6b5cf7;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}
</style>
