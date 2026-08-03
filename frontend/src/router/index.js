import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import { IS_STATIC } from '../api'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/Home.vue') },
  { path: '/mod/:slug', name: 'mod-detail', component: () => import('../views/ModDetail.vue') },
  { path: '/admin/login', name: 'admin-login', component: () => import('../views/admin/AdminLogin.vue') },
  {
    path: '/admin/mods',
    name: 'admin-mods',
    component: () => import('../views/admin/AdminMods.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/mods/new',
    name: 'admin-mod-new',
    component: () => import('../views/admin/AdminModEdit.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/mods/:id',
    name: 'admin-mod-edit',
    component: () => import('../views/admin/AdminModEdit.vue'),
    meta: { requiresAuth: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  // 静态模式部署在 GitHub Pages 子路径下，用 hash 历史保证深链接可访问
  history: IS_STATIC ? createWebHashHistory() : createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (IS_STATIC && to.path.startsWith('/admin')) {
    return { path: '/' }
  }
  if (to.meta.requiresAuth && !localStorage.getItem('mods_token')) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }
})

export default router
