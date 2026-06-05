import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/Chat.vue')
  },
  {
    path: '/reader/:id?',
    name: 'Reader',
    component: () => import('@/views/Reader.vue')
  },
  {
    path: '/graph',
    name: 'Graph',
    component: () => import('@/views/Graph.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
