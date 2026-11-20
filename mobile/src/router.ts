import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from './utils/native'

const routes = [
  { path: '/login', component: () => import('./views/Login.vue'), meta: { title: '登录' } },
  { path: '/', redirect: '/home' },
  { path: '/home', component: () => import('./views/Home.vue'), meta: { title: '首页' } },
  { path: '/approvals', component: () => import('./views/Approvals.vue'), meta: { title: '待办审批' } },
  { path: '/tasks', component: () => import('./views/Tasks.vue'), meta: { title: '我的任务' } },
  { path: '/stock', component: () => import('./views/Stock.vue'), meta: { title: '库存查询' } },
  { path: '/leads', component: () => import('./views/Leads.vue'), meta: { title: '销售线索' } },
  { path: '/notices', component: () => import('./views/Notices.vue'), meta: { title: '公告' } },
  { path: '/me', component: () => import('./views/Me.vue'), meta: { title: '我的' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + 'EBMP'
  if (to.path === '/login') return true
  if (!getToken()) return { path: '/login', query: { redirect: to.fullPath } }
  return true
})

export default router
