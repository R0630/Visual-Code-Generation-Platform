import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

import HomePage from '@/pages/HomePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import AppManagePage from '@/pages/admin/AppManagePage.vue'
import ChatManagePage from '@/pages/admin/ChatManagePage.vue'
import AppChatPage from '@/pages/app/AppChatPage.vue'
import AppEditPage from '@/pages/app/AppEditPage.vue'

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: '主页',
    component: HomePage,
    meta: { title: '首页' },
  },
  {
    path: '/user/login',
    name: '用户登录',
    component: UserLoginPage,
    meta: { title: '用户登录', hideInMenu: true },
  },
  {
    path: '/user/register',
    name: '用户注册',
    component: UserRegisterPage,
    meta: { title: '用户注册', hideInMenu: true },
  },
  {
    path: '/admin/userManage',
    name: '用户管理',
    component: UserManagePage,
    meta: { title: '用户管理' },
  },
  {
    path: '/admin/appManage',
    name: '应用管理',
    component: AppManagePage,
    meta: { title: '应用管理' },
  },
  {
    path: '/admin/chatManage',
    name: '对话管理',
    component: ChatManagePage,
    meta: { title: '对话管理' },
  },
  {
    // 对话页：登录后可用，不出现在顶部菜单
    path: '/app/chat/:id',
    name: '应用对话',
    component: AppChatPage,
    meta: { title: '应用对话', hideInMenu: true, requireLogin: true },
  },
  {
    // 编辑应用：登录后可用，不出现在顶部菜单
    path: '/app/edit/:id',
    name: '编辑应用',
    component: AppEditPage,
    meta: { title: '编辑应用', hideInMenu: true, requireLogin: true },
  },
  {
    // 兜底路由：未匹配的地址回到首页
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.afterEach((to) => {
  document.title = `${to.meta.title ?? '页面'} - 星构 AI`
})

export default router
