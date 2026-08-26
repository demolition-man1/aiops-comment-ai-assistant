import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

export const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to) {
    if (to.hash) {
      return { el: to.hash, top: 80, behavior: 'smooth' }
    }
    return { top: 0 }
  },
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'comments', name: 'comments', component: () => import('@/views/CommentWorkbenchView.vue') },
        { path: 'tags', name: 'tags', component: () => import('@/views/TagLibraryView.vue') },
        { path: 'solutions', name: 'solutions', component: () => import('@/views/ProblemSolutionView.vue') },
        { path: 'import', name: 'import', component: () => import('@/views/DataImportView.vue') },
        { path: 'sync', name: 'sync', component: () => import('@/views/SyncCenterView.vue') },
        { path: 'tasks', name: 'tasks', component: () => import('@/views/TaskCenterView.vue') },
        { path: 'reports', name: 'reports', component: () => import('@/views/ReportsView.vue') },
        { path: 'compare', name: 'compare', component: () => import('@/views/ProductCompareView.vue') },
        { path: 'content', name: 'content', component: () => import('@/views/AiContentView.vue') },
        { path: 'alerts', name: 'alerts', component: () => import('@/views/AlertCenterView.vue') },
        { path: 'settings', name: 'settings', component: () => import('@/views/SettingsView.vue') }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return { name: 'login' }
  }
  if (to.name === 'login' && auth.isLoggedIn) {
    return { name: 'dashboard' }
  }
  return true
})
