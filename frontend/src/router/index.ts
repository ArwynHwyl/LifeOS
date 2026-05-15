import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/features/courses/views/CourseManagementView.vue'),
    },
    {
      path: '/courses/:id',
      component: () => import('@/features/courses/views/CourseDetailView.vue'),
    },
  ],
})

export default router
