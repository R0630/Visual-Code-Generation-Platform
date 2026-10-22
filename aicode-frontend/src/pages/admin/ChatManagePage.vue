<template>
  <div class="page-shell chat-manage-page">
    <h2 class="section-title">对话管理</h2>
    <p class="page-desc">
      【这里写一句管理页说明，例如：在这里查看所有应用的对话记录，排查用户与 AI 的交互问题。】
    </p>

    <!-- 搜索表单 -->
    <div class="purple-card search-card">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="应用 ID">
          <a-input
            v-model:value="searchParams.appId"
            placeholder="输入应用 ID"
            allow-clear
            style="width: 200px"
          />
        </a-form-item>
        <a-form-item label="用户 ID">
          <a-input
            v-model:value="searchParams.userId"
            placeholder="输入用户 ID"
            allow-clear
            style="width: 200px"
          />
        </a-form-item>
        <a-form-item label="消息类型">
          <a-select
            v-model:value="searchParams.messageType"
            :options="messageTypeOptions"
            style="width: 140px"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="doReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>

    <!-- 对话记录表格 -->
    <div class="purple-card table-card">
      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1100 }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'message'">
            <a-tooltip :title="record.message" placement="topLeft">
              <div class="message-text">{{ record.message }}</div>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'messageType'">
            <a-tag v-if="record.messageType === 'user'" color="blue">用户</a-tag>
            <a-tag v-else-if="record.messageType === 'ai'" color="purple">AI</a-tag>
            <span v-else class="muted-text">{{ record.messageType || '未知' }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { chatHistoryControllerListAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'
import { formatTime } from '@/utils/time'

/** 表格列定义 */
const columns = [
  { title: 'ID', dataIndex: 'id', width: 180, fixed: 'left' },
  { title: '消息内容', dataIndex: 'message', width: 320 },
  { title: '消息类型', dataIndex: 'messageType', width: 100 },
  { title: '应用 ID', dataIndex: 'appId', width: 180 },
  { title: '用户 ID', dataIndex: 'userId', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
]

/** 消息类型下拉：额外带一个「全部」 */
const messageTypeOptions = [
  { label: '全部', value: '' },
  { label: '用户消息', value: 'user' },
  { label: 'AI 消息', value: 'ai' },
]

/** 列表数据 */
const dataList = ref<API.ChatHistory[]>([])
const total = ref(0)
const loading = ref(false)

/** 搜索条件 */
const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  appId: '',
  userId: '',
  messageType: '',
})

/** 分页配置 */
const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (count: number) => `共 ${count} 条`,
}))

/** 获取列表数据 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await chatHistoryControllerListAllChatHistoryByPageForAdmin({
      ...searchParams,
      // 空字符串不参与筛选
      appId: searchParams.appId || undefined,
      userId: searchParams.userId || undefined,
      messageType: searchParams.messageType || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取对话记录列表失败', error)
  } finally {
    loading.value = false
  }
}

/** 搜索（重置到第 1 页） */
const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

/** 重置搜索条件 */
const doReset = () => {
  searchParams.appId = ''
  searchParams.userId = ''
  searchParams.messageType = ''
  doSearch()
}

/** 表格换页 / 改变每页条数 */
const handleTableChange = (pag: { current?: number; pageSize?: number }) => {
  searchParams.pageNum = pag.current ?? 1
  searchParams.pageSize = pag.pageSize ?? 10
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.search-card {
  margin-bottom: 24px;
}

.table-card {
  padding: 8px 16px 16px;
}

.message-text {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted-text {
  color: var(--ink-300);
}
</style>
