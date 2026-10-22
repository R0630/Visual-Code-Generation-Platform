<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

// 全站统一紫色主题（ant-design-vue 设计 token）
const theme = {
  token: {
    colorPrimary: '#7c3aed',
    colorLink: '#7c3aed',
    colorSuccess: '#16a34a',
    colorWarning: '#f59e0b',
    colorError: '#ef4444',
    borderRadius: 10,
    fontSize: 14,
  },
}
</script>

<template>
  <a-config-provider :theme="theme">
    <BasicLayout />
  </a-config-provider>
</template>
