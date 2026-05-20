import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // Admin routes
    {
      path: '/',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/login',
      component: () => import('@/features/auth/views/LoginView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/register',
      component: () => import('@/features/auth/views/RegisterView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/verify-email',
      component: () => import('@/features/auth/views/VerifyEmailView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/forgot-password',
      component: () => import('@/features/auth/views/ForgotPasswordView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/reset-password',
      component: () => import('@/features/auth/views/ResetPasswordView.vue'),
      meta: { guestOnly: true }
    }
  ]
})

router.beforeEach((to) => {
  const isAuthenticated = Boolean(localStorage.getItem('token'))

  if (to.meta.requiresAuth && !isAuthenticated) {
    return '/login'
  }

  if (to.meta.guestOnly && isAuthenticated) {
    return '/'
  }
})

export default router
