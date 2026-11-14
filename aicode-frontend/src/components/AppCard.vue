<template>
  <div
    class="app-card"
    :class="{ 'app-card--featured': featured }"
    title="点击进入制作页"
    @click="handleViewChat"
    @mouseenter="onMouseEnter"
    @mouseleave="onMouseLeave"
  >
    <div class="app-preview">
      <!-- 悬浮实时预览：直接加载生成的网站产物，点击卡片任意位置进入制作页 -->
      <iframe
        v-if="hovering && livePreviewUrl"
        :src="livePreviewUrl"
        class="live-preview"
        :title="`${app?.appName ?? '应用'} 实时预览`"
        scrolling="no"
      />
      <template v-else>
        <img v-if="app?.cover" :src="app.cover" :alt="app.appName" />
        <!-- 封面占位：未设置封面时展示的插画 -->
        <div v-else class="app-placeholder">
          <img src="@/assets/illustrations/empty-illustration.svg" alt="暂无封面" />
          <span class="placeholder-hint">【建议在应用编辑页上传封面图，尺寸 400x300】</span>
        </div>
      </template>

      <!-- 悬浮操作条：实时预览时为底部渐变条，否则为居中遮罩，均只保留「查看作品」按钮 -->
      <div v-if="hovering && livePreviewUrl" class="live-actions" @click.stop>
        <span class="live-badge"><i class="live-dot"></i>实时预览</span>
        <a-button v-if="hasViewableWork" class="view-work-btn" size="middle" @click="handleViewWork">
          查看作品
        </a-button>
      </div>
      <div v-else-if="hovering && hasViewableWork" class="app-overlay" @click.stop>
        <a-button class="view-work-btn" size="large" @click="handleViewWork">查看作品</a-button>
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
import { computed, onUnmounted, ref } from 'vue'
import { getDeployUrl, getStaticPreviewUrl } from '@/config/env'

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

/** 是否有可查看的作品（已部署或有静态预览） */
const hasViewableWork = computed(() => {
  return !!(props.app?.deployKey || (props.app?.codeGenType && props.app?.id))
})

/** 悬浮实时预览：延迟 400ms 触发，避免鼠标划过时闪烁 */
const hovering = ref(false)
let hoverTimer: ReturnType<typeof setTimeout> | undefined

/**
 * 实时预览地址：/api/static/{codeGenType}_{appId}/
 * 与对话页 iframe 同一产物目录规则；已生成过代码的应用即可预览
 */
const livePreviewUrl = computed(() => {
  if (!props.app?.id || !props.app?.codeGenType) return ''
  return getStaticPreviewUrl(props.app.codeGenType, props.app.id)
})

const onMouseEnter = () => {
  if (!livePreviewUrl.value) return
  hoverTimer = setTimeout(() => {
    hovering.value = true
  }, 400)
}

const onMouseLeave = () => {
  if (hoverTimer) {
    clearTimeout(hoverTimer)
    hoverTimer = undefined
  }
  hovering.value = false
}

onUnmounted(() => {
  if (hoverTimer) {
    clearTimeout(hoverTimer)
    hoverTimer = undefined
  }
})

const handleViewChat = () => {
  emit('view-chat', props.app?.id)
}

/** 打开作品：优先走已部署地址，没有则回退到静态预览地址 */
const handleViewWork = () => {
  const deployUrl = props.app?.deployKey ? getDeployUrl(props.app.deployKey) : ''
  const previewUrl = props.app?.codeGenType && props.app?.id
    ? getStaticPreviewUrl(props.app.codeGenType, props.app.id)
    : ''
  const url = deployUrl || previewUrl
  if (url) {
    window.open(url, '_blank')
  }
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

/* 悬浮实时预览 iframe：铺满封面区域，禁止内部交互（点击交给卡片跳转） */
.live-preview {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
  pointer-events: none;
  transform-origin: top left;
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

/* 实时预览时的底部操作条 */
.live-actions {
  position: absolute;
  inset-inline: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  background: linear-gradient(180deg, transparent, rgba(46, 16, 101, 0.85));
  animation: live-actions-in 0.25s ease-out;
}

@keyframes live-actions-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.live-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
  background: rgba(124, 58, 237, 0.85);
  backdrop-filter: blur(2px);
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a7f3d0;
  animation: live-dot-pulse 1.2s ease-in-out infinite;
}

@keyframes live-dot-pulse {
  0%,
  100% {
    opacity: 0.5;
    transform: scale(0.85);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

.featured-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  margin: 0;
}

/* "查看作品"按钮：白色背景 + 紫色文字 + 放大 */
.view-work-btn {
  background: #fff !important;
  border-color: #fff !important;
  color: #6d28d9 !important;
  font-weight: 600;
  padding: 0 20px !important;
  height: 40px !important;
  font-size: 15px !important;
  border-radius: 20px !important;
  box-shadow: 0 4px 16px rgba(109, 40, 217, 0.25);
  transition: all 0.2s ease;
}

.view-work-btn:hover {
  background: #faf5ff !important;
  border-color: #a78bfa !important;
  color: #5b21b6 !important;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(109, 40, 217, 0.35);
}

.live-actions .view-work-btn {
  height: 32px !important;
  padding: 0 14px !important;
  font-size: 13px !important;
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
