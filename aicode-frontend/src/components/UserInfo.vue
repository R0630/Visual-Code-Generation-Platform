<template>
  <div class="user-info">
    <a-avatar :src="user?.userAvatar" :size="avatarSize" class="user-avatar">
      {{ user?.userName?.charAt(0) || 'U' }}
    </a-avatar>
    <span class="user-name">{{ user?.userName || '未知用户' }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  user?: API.UserVO
  size?: 'small' | 'default'
}

const props = withDefaults(defineProps<Props>(), {
  user: undefined,
  size: 'default',
})

/** 头像尺寸：small 用于表格等紧凑场景 */
const avatarSize = computed(() => (props.size === 'small' ? 24 : 32))
</script>

<style scoped>
.user-info {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.user-avatar {
  flex-shrink: 0;
  background: var(--purple-100);
  color: var(--purple-600);
  font-weight: 600;
}

.user-name {
  color: var(--ink-700);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
