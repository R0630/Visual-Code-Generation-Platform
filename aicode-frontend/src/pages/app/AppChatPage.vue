<template>
  <div class="page-shell app-chat-page">
    <!-- 顶部提示：对话历史已接入，仅应用创建者和管理员可查看 -->
    <div class="page-tips">
      <span class="placeholder-hint">
        【对话历史已自动保存；刷新页面后会加载最近 10 条记录，更早的记录可点击"加载更多"查看】
      </span>
      <span v-if="appInfo?.deployedTime" class="deployed-time">
        最近部署：{{ formatTime(appInfo.deployedTime) }}
      </span>
    </div>

    <div class="chat-layout">
      <!-- 左侧：对话区 -->
      <section class="purple-card chat-panel">
        <header class="panel-header">
          <div class="panel-title-wrap">
            <h2 class="panel-title gradient-text">{{ appInfo?.appName || 'AI 网站生成' }}</h2>
            <a-tooltip v-if="appInfo?.codeGenType" :title="codeGenTypeDesc">
              <a-tag :color="codeGenTypeColor">
                {{ formatCodeGenType(appInfo.codeGenType) }}
              </a-tag>
            </a-tooltip>
          </div>
          <a-button v-if="canManage" size="small" @click="editApp">
            <template #icon>
              <EditOutlined />
            </template>
            编辑应用
          </a-button>
        </header>

        <div ref="messagesContainer" class="message-list">
          <!-- 游标分页：点击加载更早的历史消息 -->
          <div v-if="hasMoreHistory" class="load-more">
            <a-button size="small" :loading="loadingMore" @click="loadMoreHistory">
              加载更多历史消息
            </a-button>
          </div>
          <div
            v-for="(item, index) in messages"
            :key="index"
            class="message-row"
            :class="item.type === 'user' ? 'is-user' : 'is-ai'"
          >
            <a-avatar v-if="item.type === 'ai'" :src="aiAvatar" :size="32" class="avatar" />
            <div class="bubble">
              <div v-if="item.loading" class="loading-line">
                <PlanetLoader :size="20" />
                <span>AI 正在生成中...</span>
              </div>
              <div v-else class="bubble-text">{{ item.content }}</div>
              <div class="bubble-time">{{ formatTime(item.createTime, 'HH:mm') }}</div>
            </div>
            <a-avatar
              v-if="item.type === 'user'"
              :src="loginUserStore.loginUser.userAvatar"
              :size="32"
              class="avatar"
            >
              {{ loginUserStore.loginUser.userName?.slice(0, 1) }}
            </a-avatar>
          </div>

          <div v-if="!messages.length && !historyLoading" class="empty-state chat-empty">
            <div class="illustration-wrap">
              <img :src="emptyIllustration" alt="暂无对话" class="empty-illustration" />
            </div>
            <p class="empty-title">还没有对话</p>
            <p class="empty-desc">【在这里描述你想要生成的网站，右侧会实时展示效果】</p>
          </div>
          <div v-if="historyLoading" class="history-loading">
            <a-spin size="small" />
            <span>正在加载对话历史...</span>
          </div>
        </div>

        <div class="composer">
          <a-textarea
            v-model:value="userInput"
            :rows="3"
            :maxlength="1000"
            :disabled="!canSend || isGenerating"
            :placeholder="inputPlaceholder"
            @keydown.enter.prevent="sendMessage"
          />
          <div class="composer-actions">
            <span v-if="!canSend" class="composer-tip">只有应用创建者可以发送消息</span>
            <span v-else-if="isGenerating" class="composer-tip">生成中，输入框已暂时锁定</span>
            <span v-else class="composer-tip">Enter 发送，Shift 可换行输入更多细节</span>
            <a-button
              type="primary"
              class="btn-purple"
              :loading="isGenerating"
              :disabled="!canSend || !userInput.trim()"
              @click="sendMessage"
            >
              <template #icon>
                <SendOutlined />
              </template>
              发送
            </a-button>
          </div>
        </div>
      </section>

      <!-- 右侧：预览区 -->
      <section class="purple-card preview-panel">
        <header class="panel-header">
          <h2 class="section-title preview-title">实时预览</h2>
          <a-space :size="8" wrap>
            <a-button size="small" :disabled="!previewUrl" @click="refreshPreview">
              <template #icon>
                <ReloadOutlined />
              </template>
              刷新预览
            </a-button>
            <a-button size="small" :disabled="!previewUrl" @click="openPreviewInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              新窗口打开
            </a-button>
            <a-button
              v-if="canManage"
              size="small"
              type="primary"
              class="btn-purple"
              :loading="deploying"
              @click="deployApp"
            >
              <template #icon>
                <CloudUploadOutlined />
              </template>
              部署
            </a-button>
          </a-space>
        </header>

        <div class="preview-stage">
          <iframe
            v-if="previewUrl"
            :key="previewKey"
            :src="previewUrl"
            class="preview-iframe"
            title="网站预览"
          />
          <div v-else class="empty-state">
            <div class="illustration-wrap">
              <img :src="astrolabe" alt="等待构筑" class="empty-deco" />
            </div>
            <p class="empty-title">还没有生成内容</p>
            <p class="empty-desc">
              【对话产生至少 2 条记录（一轮问答）后，这里会展示生成的网站效果】
            </p>
          </div>

          <!-- 生成中动画覆盖层：星球与黑猫分阶段出现，避免并列拥挤 -->
          <div v-if="isGenerating" class="generating-overlay">
            <div v-if="generatingStage === 'planet'" class="generating-planet">
              <PlanetLoader :size="130" />
            </div>
            <div v-else class="generating-cat">
              <CatLoader text="小黑猫正在敲代码" />
            </div>
            <p class="generating-tip">
              {{ generatingTip }}<span class="generating-dots"><i></i><i></i><i></i></span>
            </p>
          </div>
        </div>
      </section>
    </div>

    <!-- 部署成功弹窗：突出公网访问链接 -->
    <a-modal v-model:open="deployModalVisible" title="部署成功" :footer="null" width="560px">
      <div class="deploy-result">
        <div class="deploy-success-head">
          <CheckCircleFilled class="deploy-success-icon" />
          <div>
            <p class="deploy-success-title">部署成功！你的网站已公开上线</p>
            <p class="deploy-success-desc">任何人在浏览器打开下方链接即可直接访问使用</p>
          </div>
        </div>

        <div class="deploy-link-box">
          <span class="deploy-link-label">公开访问地址</span>
          <a-typography-paragraph class="deploy-link-text" copyable>{{ deployedUrl }}</a-typography-paragraph>
          <div class="deploy-actions">
            <a-button type="primary" class="btn-purple" @click="openDeployedSite">
              <template #icon>
                <ExportOutlined />
              </template>
              打开网站
            </a-button>
            <a-button @click="copyLink(deployedUrl)">复制链接</a-button>
          </div>
        </div>

        <div class="deploy-sub">
          <span class="deploy-sub-label">静态预览地址</span>
          <a-typography-paragraph class="deploy-sub-text" copyable>
            {{ staticPreviewUrl }}
          </a-typography-paragraph>
        </div>
        <p class="deploy-tip">
          <span class="placeholder-hint">
            【正式部署域名请在后端 AppConstant.CODE_DEPLOY_HOST 配置】
          </span>
        </p>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CheckCircleFilled,
  CloudUploadOutlined,
  EditOutlined,
  ExportOutlined,
  ReloadOutlined,
  SendOutlined,
} from '@ant-design/icons-vue'
import {
  appControllerDeployApp,
  appControllerGetAppVoById,
} from '@/api/appController'
import { chatHistoryControllerListAppChatHistory } from '@/api/chatHistoryController'
import { useLoginUserStore } from '@/stores/loginUser'
import { API_BASE_URL, getDeployUrl, getStaticPreviewUrl } from '@/config/env'
import { CODE_GEN_TYPE_CONFIG, CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'
import request from '@/request'
import PlanetLoader from '@/components/PlanetLoader.vue'
import CatLoader from '@/components/CatLoader.vue'
// AI 头像与装饰素材：需要替换时直接覆盖 assets 下的同名文件
import aiAvatar from '@/assets/illustrations/ai-avatar.svg'
import emptyIllustration from '@/assets/illustrations/empty-illustration.svg'
import astrolabe from '@/assets/decor/astrolabe.png'

/** 页面内维护的一条对话消息 */
interface ChatMessage {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

/** 应用信息 */
const appInfo = ref<API.AppVO>()
const appId = ref('')

/** 对话相关 */
const messages = ref<ChatMessage[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
/** 初始提示词只自动发送一次 */
const initialPromptSent = ref(false)

/**
 * 对话历史（游标分页）：
 * - 服务端按 createTime 降序返回，前端反转成升序展示
 * - lastCreateTime 取当前已加载最早一条的创建时间，作为下一页游标
 */
const HISTORY_PAGE_SIZE = 10
const historyLoading = ref(false)
const loadingMore = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
/** 服务端该应用的对话记录总数（首轮加载时写入），用于决定是否展示网站预览 */
const chatRecordCount = ref(0)

/** 预览相关 */
const previewKey = ref(0)

/** 部署相关 */
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployedUrl = ref('')

/** 当前 SSE 连接，组件卸载时需要关闭 */
let activeEventSource: EventSource | null = null

/** 是否为应用作者（只有作者能对话） */
const isOwner = computed(
  () => !!appInfo.value?.userId && appInfo.value.userId === loginUserStore.loginUser.id,
)

/** 是否为管理员 */
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

/** 能否发送消息 */
const canSend = computed(() => isOwner.value)

/** 能否管理（编辑 / 部署） */
const canManage = computed(() => isOwner.value || isAdmin.value)

/**
 * 静态预览地址：/api/static/{codeGenType}_{appId}/
 * 后端产物目录名由 codeGenType + appId 组成，用 deployKey 会 404
 */
const staticPreviewUrl = computed(() =>
  getStaticPreviewUrl(appInfo.value?.codeGenType, appInfo.value?.id),
)

/** iframe 展示地址：应用至少有 2 条对话记录（完成过一轮问答）或已部署过才展示 */
const previewUrl = computed(() => {
  const hasConversation = chatRecordCount.value >= 2 || messages.value.length >= 2
  const hasDeployed = !!appInfo.value?.deployedTime
  return hasConversation || hasDeployed ? staticPreviewUrl.value : ''
})

const inputPlaceholder = computed(() => {
  if (!canSend.value) return '只有应用创建者可以发送消息'
  if (isGenerating.value) return 'AI 正在生成中，请稍候...'
  return '描述你想生成的网站，越详细效果越好'
})

/** 生成过程中的阶段提示（随时间轮播），星球与黑猫交替分阶段展示 */
const GENERATING_TIPS = [
  '星球开始转动，正在理解你的需求',
  '正在设计页面结构与配色',
  '小黑猫敲代码中，正在编写页面',
  '正在打磨细节，即将完成',
] as const

const generatingTipIndex = ref(0)
let tipTimer: ReturnType<typeof setInterval> | undefined

watch(isGenerating, (active) => {
  if (active) {
    generatingTipIndex.value = 0
    tipTimer = setInterval(() => {
      generatingTipIndex.value = (generatingTipIndex.value + 1) % GENERATING_TIPS.length
    }, 2600)
  } else if (tipTimer) {
    clearInterval(tipTimer)
    tipTimer = undefined
  }
})

const generatingTip = computed(() => GENERATING_TIPS[generatingTipIndex.value] ?? '正在努力生成中')

/** 生成类型标签：颜色与说明跟随应用实际存储的 codeGenType 变化 */
const codeGenTypeColor = computed(() => {
  const map: Record<string, string> = {
    html: 'cyan',
    multi_file: 'purple',
    vue_project: 'geekblue',
  }
  return map[appInfo.value?.codeGenType ?? ''] ?? 'default'
})

const codeGenTypeDesc = computed(() => {
  const config = CODE_GEN_TYPE_CONFIG[appInfo.value?.codeGenType as CodeGenTypeEnum]
  return config ? `创建应用时选择的生成模式：${config.desc}` : ''
})

/** 前两个阶段展示星球动画，后两个阶段切换为黑猫动画 */
const generatingStage = computed(() => (generatingTipIndex.value < 2 ? 'planet' : 'cat'))

/** 关闭 SSE 连接 */
const closeEventSource = () => {
  activeEventSource?.close()
  activeEventSource = null
}

/** 滚动到消息列表底部 */
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

/** 更新指定下标的 AI 消息 */
const updateAiMessage = (index: number, patch: Partial<ChatMessage>) => {
  const target = messages.value[index]
  if (target) {
    Object.assign(target, patch)
  }
}

/** 追加一条用户消息 */
const appendUserMessage = (content: string) => {
  messages.value.push({
    type: 'user',
    content,
    createTime: new Date().toISOString(),
  })
}

/** 追加一条 AI 占位消息，返回其下标 */
const appendAiPlaceholder = () => {
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
    createTime: new Date().toISOString(),
  })
  return messages.value.length - 1
}

/** 刷新预览（递增 key 强制 iframe 重新加载） */
const updatePreview = () => {
  previewKey.value += 1
}

const refreshPreview = () => {
  if (!previewUrl.value) return
  updatePreview()
  message.success('预览已刷新')
}

/** 获取应用信息 */
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用 ID 不存在')
    router.push('/')
    return
  }
  appId.value = id
  try {
    const res = await appControllerGetAppVoById({ id })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
    } else {
      message.error('获取应用信息失败：' + res.data.message)
    }
  } catch (error) {
    console.error('获取应用信息失败', error)
    message.error('获取应用信息失败')
  }
}

/** 把一条后端历史记录转换为页面消息结构 */
const toChatMessage = (record: API.ChatHistory): ChatMessage => ({
  type: record.messageType === 'user' ? 'user' : 'ai',
  content: record.message ?? '',
  createTime: record.createTime,
})

/**
 * 首次进入加载最近一页对话历史（游标查询，按创建时间升序展示）
 * 仅应用创建者和管理员有权限调用
 */
const loadHistory = async () => {
  if (!appId.value || !canManage.value) return
  historyLoading.value = true
  try {
    const res = await chatHistoryControllerListAppChatHistory({
      appId: appId.value,
      pageSize: HISTORY_PAGE_SIZE,
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records ?? []
      chatRecordCount.value = res.data.data.totalRow ?? records.length
      hasMoreHistory.value = records.length >= HISTORY_PAGE_SIZE
      // 服务端降序返回，反转成升序
      messages.value = [...records].reverse().map(toChatMessage)
      lastCreateTime.value = records.at(-1)?.createTime
      scrollToBottom()
    } else {
      message.error('加载对话历史失败：' + res.data.message)
    }
  } catch (error) {
    console.error('加载对话历史失败', error)
    message.error('加载对话历史失败')
  } finally {
    historyLoading.value = false
  }
}

/** 游标加载更早的一页历史消息，插入到列表顶部 */
const loadMoreHistory = async () => {
  if (!lastCreateTime.value || !hasMoreHistory.value || loadingMore.value) return
  loadingMore.value = true
  // 记录加载前的滚动状态，插入后补偿偏移，避免视口跳动
  const container = messagesContainer.value
  const prevScrollHeight = container?.scrollHeight ?? 0
  try {
    const res = await chatHistoryControllerListAppChatHistory({
      appId: appId.value,
      pageSize: HISTORY_PAGE_SIZE,
      lastCreateTime: lastCreateTime.value,
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records ?? []
      hasMoreHistory.value = records.length >= HISTORY_PAGE_SIZE
      const olderMessages = [...records].reverse().map(toChatMessage)
      messages.value = [...olderMessages, ...messages.value]
      if (records.length) {
        lastCreateTime.value = records.at(-1)?.createTime
      }
      await nextTick()
      if (container) {
        container.scrollTop += container.scrollHeight - prevScrollHeight
      }
    } else {
      message.error('加载更多失败：' + res.data.message)
    }
  } catch (error) {
    console.error('加载更多历史消息失败', error)
    message.error('加载更多历史消息失败')
  } finally {
    loadingMore.value = false
  }
}

/** 生成结束后的收尾：重新拉取应用信息并刷新预览 */
const finishGeneration = () => {
  // 本轮问答会在服务端落库 2 条记录（用户 + AI），同步本地计数以驱动预览展示
  chatRecordCount.value += 2
  window.setTimeout(async () => {
    await fetchAppInfo()
    updatePreview()
  }, 1000)
}

/** 生成失败时的统一处理 */
const handleGenerateError = (aiMessageIndex: number, reason: string) => {
  isGenerating.value = false
  closeEventSource()
  updateAiMessage(aiMessageIndex, { content: reason, loading: false })
  message.error(reason)
}

/**
 * 调用 SSE 接口流式生成代码
 * 接口：GET {baseURL}/app/chat/gen/code?appId=xxx&message=xxx
 * 消息体：{"d":"文本片段"}；结束事件：done
 */
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let streamCompleted = false
  try {
    const baseURL = request.defaults.baseURL || API_BASE_URL
    const params = new URLSearchParams({
      appId: appId.value,
      message: userMessage,
    })
    const eventSource = new EventSource(`${baseURL}/app/chat/gen/code?${params.toString()}`, {
      withCredentials: true,
    })
    activeEventSource = eventSource

    let fullContent = ''

    eventSource.onmessage = (event: MessageEvent) => {
      if (streamCompleted) return
      try {
        const parsed = JSON.parse(event.data) as { d?: string }
        const chunk = parsed.d
        if (chunk) {
          fullContent += chunk
          updateAiMessage(aiMessageIndex, { content: fullContent, loading: false })
          scrollToBottom()
        }
      } catch (error) {
        console.error('解析 SSE 消息失败', error)
        streamCompleted = true
        handleGenerateError(aiMessageIndex, '生成内容解析失败，请重试')
      }
    }

    // 正常结束
    eventSource.addEventListener('done', () => {
      if (streamCompleted) return
      streamCompleted = true
      isGenerating.value = false
      closeEventSource()
      finishGeneration()
    })

    // 后端业务异常（如限流）兜底
    eventSource.addEventListener('business-error', (event: Event) => {
      if (streamCompleted) return
      try {
        const parsed = JSON.parse((event as MessageEvent).data) as { message?: string }
        const errorMsg = parsed.message ?? '生成过程中出现错误'
        streamCompleted = true
        isGenerating.value = false
        closeEventSource()
        updateAiMessage(aiMessageIndex, { content: `生成失败：${errorMsg}`, loading: false })
        message.error('生成失败：' + errorMsg)
      } catch (error) {
        console.error('解析 SSE 错误事件失败', error)
        streamCompleted = true
        handleGenerateError(aiMessageIndex, '生成过程中出现错误，请重试')
      }
    })

    eventSource.onerror = () => {
      if (streamCompleted) return
      // 连接被服务端关闭（readyState 回到 CONNECTING）视为正常结束
      if (eventSource.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        closeEventSource()
        finishGeneration()
      } else {
        streamCompleted = true
        handleGenerateError(aiMessageIndex, '生成连接异常，请重试')
      }
    }
  } catch (error) {
    console.error('创建 SSE 连接失败', error)
    handleGenerateError(aiMessageIndex, '生成失败，请稍后重试')
  }
}

/** 发起一轮生成（已追加用户消息后调用） */
const startGeneration = async (content: string) => {
  const aiMessageIndex = appendAiPlaceholder()
  isGenerating.value = true
  await nextTick()
  scrollToBottom()
  await generateCode(content, aiMessageIndex)
}

/** 发送消息 */
const sendMessage = async () => {
  const content = userInput.value.trim()
  if (!content || isGenerating.value || !canSend.value) return
  userInput.value = ''
  appendUserMessage(content)
  await startGeneration(content)
}

/** 首次进入时自动发送应用的初始提示词 */
const sendInitialMessage = async (prompt: string) => {
  appendUserMessage(prompt)
  await startGeneration(prompt)
}

/** 新窗口打开预览 */
const openPreviewInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

/** 打开已部署的网站 */
const openDeployedSite = () => {
  if (deployedUrl.value) {
    window.open(deployedUrl.value, '_blank')
  }
}

/** 复制部署链接 */
const copyLink = async (url: string) => {
  if (!url) return
  try {
    await navigator.clipboard.writeText(url)
    message.success('链接已复制')
  } catch (error) {
    console.error('复制链接失败', error)
    message.error('复制失败，请手动复制：' + (error as Error).message)
  }
}

/** 部署应用 */
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用 ID 不存在')
    return
  }
  deploying.value = true
  try {
    const res = await appControllerDeployApp({ appId: appId.value })
    if (res.data.code === 0 && res.data.data) {
      deployedUrl.value = getDeployUrl(res.data.data)
      deployModalVisible.value = true
      message.success('部署成功')
      await fetchAppInfo()
      updatePreview()
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败', error)
    message.error('部署失败，请稍后重试')
  } finally {
    deploying.value = false
  }
}

/** 跳转编辑应用 */
const editApp = () => {
  if (appId.value) {
    router.push(`/app/edit/${appId.value}`)
  }
}

onMounted(async () => {
  await fetchAppInfo()
  // 加载最近 10 条对话历史（创建者/管理员可见）
  await loadHistory()
  // 应用作者且没有任何对话历史时，自动发送初始提示词开启第一轮对话
  if (
    appInfo.value?.initPrompt &&
    canSend.value &&
    !messages.value.length &&
    !initialPromptSent.value
  ) {
    initialPromptSent.value = true
    await sendInitialMessage(appInfo.value.initPrompt)
  }
})

onUnmounted(() => {
  closeEventSource()
  if (tipTimer) {
    clearInterval(tipTimer)
    tipTimer = undefined
  }
})
</script>

<style scoped>
.app-chat-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-tips {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.deployed-time {
  color: var(--ink-500);
  font-size: 13px;
}

.chat-layout {
  display: grid;
  grid-template-columns: minmax(0, 420px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.chat-panel,
.preview-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.panel-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.panel-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.preview-title {
  margin: 0;
  font-size: 18px;
}

/* 消息列表 */
.message-list {
  height: 520px;
  overflow-y: auto;
  padding: 8px 4px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--purple-50);
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 16px;
}

.message-row.is-user {
  justify-content: flex-end;
}

.avatar {
  flex-shrink: 0;
  background: var(--purple-100);
  color: var(--purple-600);
}

.bubble {
  max-width: 78%;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.is-user .bubble {
  background: linear-gradient(135deg, var(--purple-600), var(--purple-500));
  color: #fff;
  border-bottom-right-radius: 4px;
}

.is-ai .bubble {
  background: var(--surface);
  border: 1px solid var(--line);
  color: var(--ink-900);
  border-bottom-left-radius: 4px;
}

.bubble-text {
  white-space: pre-wrap;
}

.bubble-time {
  margin-top: 6px;
  font-size: 12px;
  color: var(--ink-300);
}

.is-user .bubble-time {
  color: rgba(255, 255, 255, 0.75);
  text-align: right;
}

/* 游标加载更多 */
.load-more {
  display: flex;
  justify-content: center;
  padding: 4px 0 12px;
}

.history-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 0;
  color: var(--ink-500);
  font-size: 13px;
}

.loading-line {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--purple-600);
}

.chat-empty {
  padding: 24px 12px;
}

.empty-illustration {
  width: 160px;
  margin-bottom: 12px;
  opacity: 0.9;
}

.empty-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-700);
}

.empty-desc {
  margin: 0;
  font-size: 13px;
}

/* 输入区 */
.composer {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.composer-tip {
  font-size: 12px;
  color: var(--ink-500);
}

/* 预览区 */
.preview-stage {
  position: relative;
  height: 520px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--purple-50);
  overflow: hidden;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: var(--surface);
}

/* 预览空状态里的星象盘装饰：缓慢旋转 */
.empty-deco {
  width: 120px;
  margin-bottom: 12px;
  opacity: 0.85;
  animation: deco-spin 22s linear infinite;
  filter: drop-shadow(0 0 10px rgba(139, 92, 246, 0.35));
}

@keyframes deco-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 生成中覆盖层：星球自转 + 小黑猫 */
.generating-overlay {
  position: absolute;
  inset: 0;
  z-index: 5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 24px;
  background: rgba(250, 245, 255, 0.88);
  backdrop-filter: blur(3px);
}

.generating-planet {
  animation: planet-float 3.6s ease-in-out infinite;
}

@keyframes planet-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.generating-tip {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--purple-600);
}

.generating-dots {
  display: inline-flex;
  gap: 3px;
  margin-left: 5px;
}

.generating-dots i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  animation: generating-dot 1.2s ease-in-out infinite;
}

.generating-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.generating-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes generating-dot {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

.generating-cat {
  animation: planet-float 4.4s ease-in-out infinite reverse;
}

/* 部署弹窗 */
.deploy-result {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.deploy-success-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.1), rgba(167, 139, 250, 0.14));
  border: 1px solid var(--purple-200);
}

.deploy-success-icon {
  font-size: 30px;
  color: var(--purple-600);
}

.deploy-success-title {
  margin: 0 0 2px;
  font-size: 15px;
  font-weight: 700;
  color: var(--ink-900);
}

.deploy-success-desc {
  margin: 0;
  font-size: 13px;
  color: var(--ink-500);
}

.deploy-link-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  border: 1.5px dashed var(--purple-300, #c4b5fd);
  border-radius: var(--radius-sm);
  background: var(--purple-50);
}

.deploy-link-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--purple-600);
  letter-spacing: 1px;
}

.deploy-link-text {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
  word-break: break-all;
}

.deploy-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
}

.deploy-sub {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.deploy-sub-label {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--ink-500);
}

.deploy-sub-text {
  margin: 0;
  font-size: 12px;
  color: var(--ink-700);
  word-break: break-all;
}

.deploy-tip {
  margin: 0;
}

/* 响应式：窄屏上下堆叠 */
@media (max-width: 768px) {
  .chat-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .message-list,
  .preview-stage {
    height: 360px;
  }

  .bubble {
    max-width: 84%;
  }
}
</style>
