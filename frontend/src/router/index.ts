import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
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
