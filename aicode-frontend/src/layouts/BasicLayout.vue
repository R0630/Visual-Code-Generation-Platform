<template>
  <a-layout class="basic-layout">
    <a-layout-header class="layout-header">
      <GlobalHeader :site-title="siteTitle" :menu-items="menuItems" />
    </a-layout-header>

    <a-layout-content class="layout-content">
      <RouterView />
    </a-layout-content>

    <a-layout-footer class="layout-footer">
      <GlobalFooter />
    </a-layout-footer>
  </a-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterView } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import type { HeaderMenuItem } from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
import { routes } from '@/router'
import { useLoginUserStore } from '@/stores/loginUser'

const siteTitle = '星构 AI'

const loginUserStore = useLoginUserStore()

/**
 * 由路由自动生成菜单：
 * - meta.hideInMenu 的路由（登录 / 注册 / 对话 / 编辑）不展示
 * - /admin 开头的路由只有管理员可见
 */
const menuItems = computed<HeaderMenuItem[]>(() => {
  const isAdmin = loginUserStore.loginUser.userRole === 'admin'
  return routes
    .filter((route) => {
      const meta = route.meta ?? {}
      if (meta.hideInMenu) return false
      if (typeof route.path === 'string' && route.path.startsWith('/admin') && !isAdmin) return false
      return true
    })
    .map((route) => ({
      key: route.name as string,
      label: (route.meta?.title as string) ?? (route.name as string),
      path: route.path,
    }))
})
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: transparent;
}

.layout-header {
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 0 24px;
  height: 64px;
  line-height: 64px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
}

.layout-content {
  flex: 1;
  min-height: 0;
  background: transparent;
}

.layout-footer {
  background: transparent;
  padding: 24px;
}

@media (max-width: 768px) {
  .layout-header {
    padding: 0 16px;
  }
}
</style>
