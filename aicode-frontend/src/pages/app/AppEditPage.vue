<template>
  <div class="page-shell app-edit-page">
    <h2 class="section-title">编辑应用信息</h2>
    <p class="page-desc">普通用户可修改应用名称，管理员还能设置封面与精选状态。</p>

    <a-spin :spinning="loading">
      <!-- 基本信息编辑 -->
      <div class="purple-card">
        <h3 class="section-title card-title">基本信息</h3>
        <a-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item label="应用名称" name="appName">
            <a-input
              v-model:value="formData.appName"
              placeholder="请输入应用名称"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            label="封面链接"
            name="cover"
            extra="【建议使用 400x300 的封面图，填写 http(s) 图片链接】"
          >
            <a-input v-model:value="formData.cover" placeholder="请输入封面图片链接" allow-clear />
            <div v-if="formData.cover" class="cover-preview">
              <a-image :src="formData.cover" :width="200" :height="150" />
            </div>
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            label="优先级"
            name="priority"
            extra="【设置为 99 表示精选应用，精选会展示在首页“精选案例”】"
          >
            <a-input-number v-model:value="formData.priority" :min="0" :max="99" />
          </a-form-item>

          <a-form-item label="初始提示词">
            <a-textarea
              v-model:value="formData.initPrompt"
              placeholder="初始提示词"
              :rows="4"
              disabled
            />
            <p class="field-tip">初始提示词创建后不可修改</p>
          </a-form-item>

          <a-form-item label="生成类型">
            <a-input :value="formatCodeGenType(formData.codeGenType)" disabled />
            <p class="field-tip">生成类型创建后不可修改</p>
          </a-form-item>

          <a-form-item label="部署密钥">
            <a-input v-model:value="formData.deployKey" placeholder="未部署" disabled />
            <p class="field-tip">部署密钥由系统生成，不可修改</p>
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" :loading="submitting">保存</a-button>
              <a-button @click="resetForm">重置</a-button>
              <a-button @click="goToChat">进入对话</a-button>
              <a-button @click="goHome">返回首页</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 应用信息展示 -->
      <div class="purple-card app-info-card">
        <h3 class="section-title card-title">应用信息</h3>
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="应用 ID">{{ appInfo?.id }}</a-descriptions-item>
          <a-descriptions-item label="创建者">
            <UserInfo :user="appInfo?.user" size="small" />
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatTime(appInfo?.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="更新时间">
            {{ formatTime(appInfo?.updateTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="部署时间">
            {{ appInfo?.deployedTime ? formatTime(appInfo.deployedTime) : '未部署' }}
          </a-descriptions-item>
          <a-descriptions-item label="预览链接">
            <a-button
              v-if="appInfo?.codeGenType && appInfo?.id"
              type="link"
              size="small"
              @click="openPreview"
            >
              打开预览
            </a-button>
            <span v-else class="muted-text">暂无产物</span>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  appControllerGetAppVoById,
  appControllerUpdateApp,
  appControllerUpdateAppByAdmin,
} from '@/api/appController'
import UserInfo from '@/components/UserInfo.vue'
import { getStaticPreviewUrl } from '@/config/env'
import { formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

/** 应用详情 */
const appInfo = ref<API.AppVO>()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

/** 表单数据 */
const formData = reactive({
  appName: '',
  cover: '',
  priority: 0,
  initPrompt: '',
  codeGenType: '',
  deployKey: '',
})

/** 当前登录用户是否为管理员 */
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

/** 表单校验规则 */
const rules = {
  appName: [
    { required: true, message: '请输入应用名称', trigger: 'blur' as const },
    { min: 1, max: 50, message: '应用名称长度需为 1-50 个字符', trigger: 'blur' as const },
  ],
  cover: [
    {
      pattern: /^https?:\/\/\S+$/,
      message: '请输入合法的封面链接（以 http:// 或 https:// 开头）',
      trigger: 'blur' as const,
    },
  ],
  priority: [
    { required: true, message: '请输入优先级', trigger: 'change' as const },
    {
      type: 'number' as const,
      min: 0,
      max: 99,
      message: '优先级范围为 0-99',
      trigger: 'change' as const,
    },
  ],
}

/** 从路由参数中取应用 ID */
const getAppId = (): string => {
  const rawId = route.params.id
  return Array.isArray(rawId) ? (rawId[0] ?? '') : (rawId ?? '')
}

/** 用应用详情回填表单 */
const fillForm = () => {
  formData.appName = appInfo.value?.appName ?? ''
  formData.cover = appInfo.value?.cover ?? ''
  formData.priority = appInfo.value?.priority ?? 0
  formData.initPrompt = appInfo.value?.initPrompt ?? ''
  formData.codeGenType = appInfo.value?.codeGenType ?? ''
  formData.deployKey = appInfo.value?.deployKey ?? ''
}

/** 获取应用详情 */
const fetchAppInfo = async () => {
  const id = getAppId()
  if (!id) {
    message.error('应用 ID 不存在')
    router.push('/')
    return
  }

  loading.value = true
  try {
    const res = await appControllerGetAppVoById({ id })
    if (res.data.code !== 0 || !res.data.data) {
      message.error('获取应用信息失败，' + res.data.message)
      router.push('/')
      return
    }
    appInfo.value = res.data.data
    // 非管理员且不是作者时无编辑权限
    if (!isAdmin.value && appInfo.value.userId !== loginUserStore.loginUser.id) {
      message.error('您没有权限编辑此应用')
      router.push('/')
      return
    }
    fillForm()
  } catch (error) {
    console.error('获取应用信息失败', error)
    message.error('获取应用信息失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

/** 提交保存 */
const handleSubmit = async () => {
  const id = appInfo.value?.id
  if (!id) return

  submitting.value = true
  try {
    const res = isAdmin.value
      ? await appControllerUpdateAppByAdmin({
          id,
          appName: formData.appName,
          cover: formData.cover,
          priority: formData.priority,
        })
      : await appControllerUpdateApp({ id, appName: formData.appName })

    if (res.data.code === 0) {
      message.success('保存成功')
      await fetchAppInfo()
    } else {
      message.error('保存失败，' + res.data.message)
    }
  } catch (error) {
    console.error('保存应用信息失败', error)
    message.error('保存失败')
  } finally {
    submitting.value = false
  }
}

/** 重置表单到初始数据 */
const resetForm = () => {
  fillForm()
  formRef.value?.clearValidate()
}

/** 进入对话页 */
const goToChat = () => {
  if (appInfo.value?.id) {
    router.push(`/app/chat/${appInfo.value.id}`)
  }
}

/** 返回首页 */
const goHome = () => {
  router.push('/')
}

/** 打开静态预览：/api/static/{codeGenType}_{appId}/ */
const openPreview = () => {
  const url = getStaticPreviewUrl(appInfo.value?.codeGenType, appInfo.value?.id)
  if (!url) {
    message.info('该应用还没有生成产物')
    return
  }
  window.open(url, '_blank')
}

onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped>
.app-edit-page {
  max-width: 900px;
}

.card-title {
  font-size: 18px;
}

.card-title::before {
  height: 18px;
}

.app-info-card {
  margin-top: 24px;
}

.cover-preview {
  margin-top: 12px;
  padding: 12px;
  border: 1px dashed var(--purple-300);
  border-radius: var(--radius-sm);
  background: var(--purple-50);
}

.field-tip {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-300);
}

.muted-text {
  color: var(--ink-300);
}
</style>
