import { createRouter, createWebHistory } from 'vue-router'
import JobListView from '../views/JobListView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'jobs', component: JobListView },
    { path: '/gap', name: 'gap', component: () => import('../views/GapView.vue') },
    { path: '/path', name: 'path', component: () => import('../views/PathView.vue') },
    { path: '/quiz', name: 'quiz', component: () => import('../views/QuizView.vue') },
  ],
})

export default router
