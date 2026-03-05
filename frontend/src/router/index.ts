import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Home',
          component: () => import('@/views/Home.vue')
        },
        {
          path: 'upload',
          name: 'Upload',
          component: () => import('@/views/Upload.vue')
        },
        {
          path: 'interview/:id',
          name: 'InterviewDetail',
          component: () => import('@/views/InterviewDetail.vue')
        },
        {
          path: 'review',
          name: 'Review',
          component: () => import('@/views/Review.vue')
        },
        {
          path: 'search',
          name: 'Search',
          component: () => import('@/views/Search.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    next('/')
  } else {
    next()
  }
})

export default router
