import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/features/courses/views/CourseManagementView.vue'),
    },
  ],
})

export default router
