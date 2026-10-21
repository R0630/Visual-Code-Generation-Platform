<template>
  <div class="global-header">
    <div class="header-left">
      <img src="@/assets/logo.svg" alt="logo" class="logo" />
      <span class="site-title">{{ siteTitle }}</span>
    </div>

    <div class="header-center">
      <a-menu
        v-model:selected-keys="selectedKeys"
        mode="horizontal"
        :items="menuItems"
        @click="handleMenuClick"
      />
    </div>

    <div class="header-right">
      <a-button type="primary">登录</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'

interface HeaderMenuItem {
  key: string
  label: string
  path?: string
}

const props = withDefaults(
  defineProps<{
    siteTitle?: string
    menuItems?: HeaderMenuItem[]
  }>(),
  {
    siteTitle: 'AI Code',
    menuItems: () => [],
  },
)

const router = useRouter()

const selectedKeys = ref<string[]>([])

const updateSelectedKey = () => {
  const current = router.currentRoute.value
  if (current.name) {
    selectedKeys.value = [current.name as string]
  }
}

updateSelectedKey()

const removeAfterEach = router.afterEach(() => {
  updateSelectedKey()
})

onBeforeUnmount(() => {
  removeAfterEach()
})

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  const item = props.menuItems.find((i) => i.key === key)
  if (item?.path) {
    router.push(item.path)
  }
}
</script>

<style scoped>
.global-header {
  display: flex;
  align-items: center;
  height: 100%;
  width: 100%;
  gap: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.logo {
  width: 32px;
  height: 32px;
}

.site-title {
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
  color: rgba(0, 0, 0, 0.88);
}

.header-center {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.header-center :deep(.ant-menu) {
  border-bottom: none;
  line-height: 64px;
}

.header-right {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .site-title {
    display: none;
  }

  .global-header {
    gap: 12px;
  }
}

@media (max-width: 576px) {
  .header-center {
    display: none;
  }
}
</style>
