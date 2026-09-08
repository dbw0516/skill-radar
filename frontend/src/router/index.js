import { createRouter, createWebHistory } from 'vue-router'
import JobListView from '../views/JobListView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'jobs', component: JobListView },
    { path: '/gap', name: 'gap', component: () => import('../views/GapView.vue'), meta: { requiresAuth: true } },
    { path: '/path', name: 'path', component: () => import('../views/PathView.vue'), meta: { requiresAuth: true } },
    { path: '/quiz', name: 'quiz', component: () => import('../views/QuizView.vue'), meta: { requiresAuth: true } },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
  ],
})

// 技能差距/学习路径/测评都要挂在一个具体用户身上，没登录就跳去登录页，
// 顺便把想去的页面记下来，登录完给他弹回来。
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !useAuthStore().isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
