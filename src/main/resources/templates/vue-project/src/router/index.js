import { createRouter, createWebHashHistory } from 'vue-router'

// 路由配置 - 由框架模板自动生成基础配置
// AI 业务代码会追加具体的页面路由
const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/pages/Home.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
