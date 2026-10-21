<template>
  <a-layout class="basic-layout">
    <a-layout-header class="layout-header">
      <GlobalHeader :site-title="siteTitle" :menu-items="menuItems" />
    </a-layout-header>

    <a-layout-content class="layout-content">
      <div class="content-wrapper">
        <RouterView />
      </div>
    </a-layout-content>

    <a-layout-footer class="layout-footer">
      <GlobalFooter />
    </a-layout-footer>
  </a-layout>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
import { routes } from '@/router'

const siteTitle = 'AI Code'

const menuItems = routes
  .filter((route) => route.meta?.title)
  .map((route) => ({
    key: route.name as string,
    label: route.meta!.title as string,
    path: route.path,
  }))
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-header {
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.layout-content {
  flex: 1;
  min-height: 0;
}

.content-wrapper {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
}

.layout-footer {
  background: #f0f2f5;
  padding: 24px;
}

@media (max-width: 768px) {
  .layout-header {
    padding: 0 16px;
  }

  .content-wrapper {
    padding: 16px;
  }
}
</style>
