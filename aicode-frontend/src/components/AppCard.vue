<template>
  <div class="app-card" :class="{ 'app-card--featured': featured }">
    <div class="app-preview">
      <img v-if="app?.cover" :src="app.cover" :alt="app.appName" />
      <!-- 封面占位：未设置封面时展示的插画 -->
      <div v-else class="app-placeholder">
        <img src="@/assets/illustrations/empty-illustration.svg" alt="暂无封面" />
        <span class="placeholder-hint">【建议在应用编辑页上传封面图，尺寸 400x300】</span>
      </div>

      <div class="app-overlay">
        <a-space>
          <a-button type="primary" @click="handleViewChat">查看对话</a-button>
          <a-button v-if="app?.deployKey" @click="handleViewWork">查看作品</a-button>
        </a-space>
      </div>

      <a-tag v-if="featured" color="purple" class="featured-tag">精选</a-tag>
    </div>

    <div class="app-info">
      <a-avatar :src="app?.user?.userAvatar" :size="40" class="app-avatar">
        {{ app?.user?.userName?.charAt(0) || 'U' }}
      </a-avatar>
      <div class="app-meta">
        <h3 class="app-title">{{ app?.appName || '未命名应用' }}</h3>
        <p class="app-author">
          {{ app?.user?.userName || (featured ? '官方' : '未知用户') }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  app: API.AppVO
  featured?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
})

const emit = defineEmits<{
  (e: 'view-chat', appId: string | undefined): void
  (e: 'view-work', app: API.AppVO): void
}>()

const handleViewChat = () => {
  emit('view-chat', props.app?.id)
}

const handleViewWork = () => {
  emit('view-work', props.app)
}
</script>

<style scoped>
.app-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition:
    transform 0.25s,
    box-shadow 0.25s;
  cursor: pointer;
}

.app-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-md);
  border-color: var(--purple-200);
}

.app-preview {
  position: relative;
  height: 180px;
  background: linear-gradient(135deg, var(--purple-50), var(--purple-100));
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.app-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
}

.app-placeholder img {
  width: 120px;
  height: auto;
}

.app-overlay {
  position: absolute;
  inset: 0;
  background: rgba(91, 33, 182, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.25s;
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.featured-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  margin: 0;
}

.app-info {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-avatar {
  flex-shrink: 0;
  background: var(--purple-100);
  color: var(--purple-600);
  font-weight: 600;
}

.app-meta {
  flex: 1;
  min-width: 0;
}

.app-title {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: var(--ink-900);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-author {
  margin: 0;
  font-size: 13px;
  color: var(--ink-500);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
