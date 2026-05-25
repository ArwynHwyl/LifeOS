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
    },
    // Admin course routes
    {
      path: '/courses',
      component: () => import('@/features/courses/views/AdminCourseManagementView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/courses/:id',
      component: () => import('@/features/courses/views/AdminCourseDetailView.vue'),
      meta: { requiresAuth: true }
    },
    // Teacher course routes
    {
      path: '/teacher/courses',
      component: () => import('@/features/courses/views/TeacherCourseManagementView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/teacher/courses/:id',
      component: () => import('@/features/courses/views/TeacherCourseDetailView.vue'),
      meta: { requiresAuth: true }
    },
    // Learner routes
    {
      path: '/learn',
      component: () => import('@/features/learning/views/LearnerLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: 'courses' },
        {
          path: 'courses',
          component: () => import('@/features/learning/views/CoursesView.vue'),
        },
        {
          path: 'lesson/:courseId',
          component: () => import('@/features/learning/views/LessonView.vue'),
        },
        {
          path: 'flashcards',
          component: () => import('@/features/learning/views/FlashcardsView.vue'),
        },
        {
          path: 'flashcards/srs',
          component: () => import('@/features/learning/views/FlashcardsSRSView.vue'),
        },
        {
          path: 'flashcards/set/:deckId',
          component: () => import('@/features/learning/views/FlashcardsSetView.vue'),
        },
        {
          path: 'dashboard',
          component: () => import('@/features/learning/views/DashboardView.vue'),
        },
      ],
    },
  ]
})

function getRoleHome(): string {
  try {
    const raw = localStorage.getItem('authUser')
    if (!raw) return '/login'
    const user = JSON.parse(raw) as { role?: string }
    if (user.role === 'ROLE_ADMIN') return '/courses'
    if (user.role === 'ROLE_TEACHER') return '/teacher/courses'
    return '/learn/courses'
  } catch {
    return '/login'
  }
}

router.beforeEach((to) => {
  const isAuthenticated = Boolean(localStorage.getItem('token'))

  if (to.meta.requiresAuth && !isAuthenticated) {
    return '/login'
  }

  if (to.meta.guestOnly && isAuthenticated) {
    return getRoleHome()
  }

  if (to.path === '/' && isAuthenticated) {
    return getRoleHome()
  }
})

export default router
