<template>
  <div class="global-header">
    <div class="header-left" @click="router.push('/')">
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

    <!-- 右侧：用户区域 -->
    <div class="header-right">
      <template v-if="loginUserStore.loginUser.id">
        <a-dropdown placement="bottomRight" trigger="click">
          <a-space class="user-info" :size="8">
            <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="28">
              {{ displayName }}
            </a-avatar>
            <span class="user-name">{{ displayName }}</span>
            <DownOutlined class="user-arrow" />
          </a-space>
          <template #overlay>
            <a-menu @click="handleUserMenuClick">
              <a-menu-item key="profile">
                <UserOutlined />
                <span class="menu-text">
                  {{ loginUserStore.loginUser.userAccount }}（{{ roleText }}）
                </span>
              </a-menu-item>
              <a-menu-item key="logout">
                <LogoutOutlined />
                <span class="menu-text">退出登录</span>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </template>
      <a-space v-else>
        <a-button type="text" @click="router.push('/user/login')">登录</a-button>
        <a-button type="primary" @click="router.push('/user/register')">注册</a-button>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { DownOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userControllerUserLogout } from '@/api/userController'

export interface HeaderMenuItem {
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
const loginUserStore = useLoginUserStore()

/** 昵称缺失时回退显示账号 */
const displayName = computed(
  () => loginUserStore.loginUser.userName || loginUserStore.loginUser.userAccount || '无名',
)

const roleText = computed(() => (loginUserStore.loginUser.userRole === 'admin' ? '管理员' : '普通用户'))

// 菜单选中项跟随当前路由
const selectedKeys = ref<string[]>([])
const updateSelectedKey = () => {
  const current = router.currentRoute.value
  selectedKeys.value = current.name ? [current.name as string] : []
}
updateSelectedKey()
const removeAfterEach = router.afterEach(() => {
  updateSelectedKey()
})
onBeforeUnmount(() => {
  if (removeAfterEach) removeAfterEach()
})

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  const item = props.menuItems.find((i) => i.key === key)
  if (item?.path) {
    router.push(item.path)
  }
}

// 用户下拉菜单
const handleUserMenuClick: MenuProps['onClick'] = async ({ key }) => {
  if (key === 'logout') {
    await doLogout()
  }
}

// 退出登录
const doLogout = async () => {
  try {
    const res = await userControllerUserLogout()
    if (res.data.code === 0) {
      message.success('已退出登录')
    } else {
      message.error('退出失败，' + (res.data.message ?? ''))
    }
  } catch (error) {
    console.error('退出登录失败', error)
  } finally {
    // 无论后端是否成功，前端都清空登录态
    loginUserStore.clearLoginUser()
    router.push('/user/login')
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
  cursor: pointer;
}

.logo {
  width: 32px;
  height: 32px;
}

.site-title {
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
  background: linear-gradient(120deg, var(--purple-600), var(--purple-400));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-center {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.header-center :deep(.ant-menu) {
  border-bottom: none;
  line-height: 62px;
}

/* 导航菜单文字加大加粗，更醒目 */
.header-center :deep(.ant-menu-item) {
  font-size: 17px;
  font-weight: 500;
}

.header-right {
  flex-shrink: 0;
}

.user-info {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: rgba(0, 0, 0, 0.04);
}

.user-name {
  white-space: nowrap;
}

.user-arrow {
  font-size: 10px;
  color: rgba(0, 0, 0, 0.45);
}

.menu-text {
  margin-left: 6px;
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

  .user-name {
    display: none;
  }
}
</style>
