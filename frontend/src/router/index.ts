import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      component: () => import('@/features/auth/views/LoginView.vue')
    },
    {
      path: '/register',
      component: () => import('@/features/auth/views/RegisterView.vue')
    }
  ]
})

export default router