<template>
  <div id="homePage">
    <!-- 顶部封面：星构星球旋转动效 + DeepSeek 式鼠标交互 -->
    <HeroCover />

    <div class="container">
      <!-- 提示词输入区 -->
      <div class="prompt-card purple-card">
        <div class="prompt-header">
          <span class="prompt-label">【描述你想做的网站，AI 帮你生成】</span>
        </div>
        <a-textarea
          v-model:value="userPrompt"
          placeholder="【默认提示词示例：帮我创建一个个人博客网站】"
          :rows="4"
          :maxlength="1000"
          class="prompt-input"
        />
        <div class="prompt-actions">
          <span class="prompt-tip">「生成过程约需 1-2 分钟，可在对话页继续修改」</span>
          <a-button
            type="primary"
            size="large"
            :loading="creating"
            :disabled="!userPrompt.trim()"
            @click="createApp"
          >
            <template #icon>
              <SendOutlined />
            </template>
            生成应用
          </a-button>
        </div>

        <!-- 快捷提示词 -->
        <div class="quick-actions">
          <a-button v-for="item in quickPrompts" :key="item.title" @click="setPrompt(item.prompt)">
            {{ item.title }}
          </a-button>
        </div>
      </div>

      <!-- 我的作品 -->
      <div class="section">
        <h2 class="section-title">我的作品</h2>
        <p class="page-desc">「你创建过的应用，点击可直接进入对话继续修改」</p>

        <template v-if="loginUserStore.loginUser.id">
          <div v-if="myApps.length" class="app-grid">
            <AppCard
              v-for="app in myApps"
              :key="app.id"
              :app="app"
              @view-chat="viewChat"
              @view-work="viewWork"
            />
          </div>
          <div v-else class="empty-state">
            <img src="@/assets/illustrations/empty-illustration.svg" alt="暂无数据" width="180" />
            <p>「还没有作品，试试在上方输入一句需求吧」</p>
          </div>
          <div class="pagination-wrapper">
            <a-pagination
              v-model:current="myAppsPage.current"
              v-model:page-size="myAppsPage.pageSize"
              :total="myAppsPage.total"
              :show-size-changer="false"
              :show-total="(total: number) => `共 ${total} 个应用`"
              @change="loadMyApps"
            />
          </div>
        </template>

        <div v-else class="empty-state">
          <img src="@/assets/illustrations/empty-illustration.svg" alt="未登录" width="180" />
          <p>「登录后即可创建并保存你的作品」</p>
          <a-button type="primary" @click="router.push('/user/login')">去登录</a-button>
        </div>
      </div>

      <!-- 精选案例 -->
      <div class="section">
        <h2 class="section-title">精选案例</h2>
        <p class="page-desc">「管理员设为精选的应用，供你参考」</p>

        <div v-if="featuredApps.length" class="app-grid">
          <AppCard
            v-for="app in featuredApps"
            :key="app.id"
            :app="app"
            featured
            @view-chat="viewChat"
            @view-work="viewWork"
          />
        </div>
        <div v-else class="empty-state">
          <img src="@/assets/illustrations/empty-illustration.svg" alt="暂无数据" width="180" />
          <p>「暂无精选案例，可在应用管理页把优先级设为 99」</p>
        </div>

        <div class="pagination-wrapper">
          <a-pagination
            v-model:current="featuredAppsPage.current"
            v-model:page-size="featuredAppsPage.pageSize"
            :total="featuredAppsPage.total"
            :show-size-changer="false"
            :show-total="(total: number) => `共 ${total} 个案例`"
            @change="loadFeaturedApps"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SendOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  appControllerAddApp,
  appControllerListGoodAppVoByPage,
  appControllerListMyAppVoByPage,
} from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'
import HeroCover from '@/components/HeroCover.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// 快捷提示词（标题 + 完整提示词，可自行增删）
const quickPrompts = [
  {
    title: '个人博客网站',
    prompt:
      '【示例提示词 1】创建一个现代化的个人博客网站，包含文章列表、详情页、分类标签、搜索功能和个人简介页面，设计简洁，支持响应式布局。',
  },
  {
    title: '企业官网',
    prompt:
      '【示例提示词 2】设计一个专业的企业官网，包含公司介绍、产品服务展示、新闻资讯、联系我们等模块，商务风格，含轮播图与产品卡片。',
  },
  {
    title: '在线商城',
    prompt:
      '【示例提示词 3】构建一个功能完整的在线商城，包含商品展示、购物车、订单管理、结算流程，商品卡片布局现代化，支持搜索筛选。',
  },
  {
    title: '作品展示网站',
    prompt:
      '【示例提示词 4】制作一个精美的作品展示网站，包含作品画廊、项目详情页、个人简历和联系方式，采用网格布局，支持图片放大预览。',
  },
]

const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

// 我的应用
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({ current: 1, pageSize: 6, total: 0 })

// 精选应用
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({ current: 1, pageSize: 6, total: 0 })

/** 创建应用：后端返回新应用 id（字符串），随后进入对话页 */
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }
  creating.value = true
  try {
    const res = await appControllerAddApp({ initPrompt: userPrompt.value.trim() })
    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      await router.push(`/app/chat/${String(res.data.data)}`)
    } else {
      message.error('创建失败，' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败', error)
  } finally {
    creating.value = false
  }
}

/** 加载我的应用 */
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) return
  try {
    const res = await appControllerListMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records ?? []
      myAppsPage.total = res.data.data.totalRow ?? 0
    }
  } catch (error) {
    console.error('加载我的应用失败', error)
  }
}

/** 加载精选应用 */
const loadFeaturedApps = async () => {
  try {
    const res = await appControllerListGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })
    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records ?? []
      featuredAppsPage.total = res.data.data.totalRow ?? 0
    }
  } catch (error) {
    console.error('加载精选应用失败', error)
  }
}

/** 进入对话页 */
const viewChat = (appId: string | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}`)
  }
}

/** 打开已部署的作品 */
const viewWork = (app: API.AppVO) => {
  if (app?.deployKey) {
    window.open(getDeployUrl(app.deployKey), '_blank')
  }
}

onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
})
</script>

<style scoped>
#homePage {
  width: 100%;
  min-height: 100vh;
}

/* 内容区 */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px 48px;
}

.prompt-card {
  margin: 8px 0 40px;
  box-shadow: var(--shadow-md);
}

.prompt-header {
  margin-bottom: 12px;
}

.prompt-label {
  font-size: 14px;
  color: var(--ink-700);
  font-weight: 600;
}

.prompt-input {
  border-radius: var(--radius-sm);
  font-size: 15px;
}

.prompt-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
}

.prompt-tip {
  font-size: 12px;
  color: var(--ink-300);
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.quick-actions .ant-btn {
  border-radius: 999px;
  background: var(--purple-50);
  border-color: var(--purple-200);
  color: var(--purple-600);
}

.quick-actions .ant-btn:hover {
  background: var(--purple-100);
  border-color: var(--purple-400);
}

/* 区块 */
.section {
  margin-bottom: 48px;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.empty-state p {
  margin: 12px 0 16px;
  color: var(--ink-500);
}

@media (max-width: 768px) {
  .prompt-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
