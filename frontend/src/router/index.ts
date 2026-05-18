import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // Admin routes
    {
      path: '/',
      component: () => import('@/features/courses/views/AdminCourseManagementView.vue'),
    },
    {
      path: '/courses/:id',
      component: () => import('@/features/courses/views/AdminCourseDetailView.vue'),
    },
    // Teacher routes
    {
      path: '/teacher',
      component: () => import('@/features/courses/views/TeacherCourseManagementView.vue'),
    },
    {
      path: '/teacher/courses/:id',
      component: () => import('@/features/courses/views/TeacherCourseDetailView.vue'),
    },
  ],
})

export default router
