<template>
  <div class="page-shell app-manage-page">
    <h2 class="section-title">应用管理</h2>
    <p class="page-desc">【在这里搜索、编辑应用，并把优秀的应用设为精选。】</p>

    <!-- 搜索表单 -->
    <div class="purple-card search-card">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="应用名称">
          <a-input
            v-model:value="searchParams.appName"
            placeholder="输入应用名称"
            allow-clear
            style="width: 180px"
          />
        </a-form-item>
        <a-form-item label="创建者用户 ID">
          <a-input
            v-model:value="searchParams.userId"
            placeholder="输入用户 ID"
            allow-clear
            style="width: 180px"
          />
        </a-form-item>
        <a-form-item label="生成类型">
          <a-select
            v-model:value="searchParams.codeGenType"
            :options="codeGenTypeOptions"
            style="width: 160px"
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

    <!-- 应用表格 -->
    <div class="purple-card table-card">
      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1400 }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'cover'">
            <a-image v-if="record.cover" :src="record.cover" :width="80" :height="60" />
            <span v-else class="placeholder-hint">【封面占位图】</span>
          </template>
          <template v-else-if="column.dataIndex === 'initPrompt'">
            <a-tooltip :title="record.initPrompt">
              <div class="prompt-text">{{ record.initPrompt }}</div>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'codeGenType'">
            {{ formatCodeGenType(record.codeGenType) }}
          </template>
          <template v-else-if="column.dataIndex === 'priority'">
            <a-tag v-if="record.priority === 99" color="gold">精选</a-tag>
            <span v-else>{{ record.priority ?? 0 }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'deployedTime'">
            <span v-if="record.deployedTime">{{ formatTime(record.deployedTime) }}</span>
            <span v-else class="muted-text">未部署</span>
          </template>
          <template v-else-if="column.dataIndex === 'user'">
            <UserInfo :user="record.user" size="small" />
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="editApp(record)">编辑</a-button>
              <a-button type="link" size="small" @click="toggleFeatured(record)">
                {{ record.priority === 99 ? '取消精选' : '精选' }}
              </a-button>
              <a-popconfirm
                title="确认删除该应用？"
                ok-text="确认删除"
                cancel-text="取消"
                @confirm="doDelete(record.id)"
              >
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  appControllerDeleteAppByAdmin,
  appControllerListAppVoByPage,
  appControllerUpdateAppByAdmin,
} from '@/api/appController'
import UserInfo from '@/components/UserInfo.vue'
import { CODE_GEN_TYPE_OPTIONS, formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'

const router = useRouter()

/** 精选应用对应的优先级 */
const FEATURED_PRIORITY = 99

/** 表格列定义 */
const columns = [
  { title: 'ID', dataIndex: 'id', width: 180, fixed: 'left' },
  { title: '应用名称', dataIndex: 'appName', width: 160 },
  { title: '封面', dataIndex: 'cover', width: 120 },
  { title: '初始提示词', dataIndex: 'initPrompt', width: 200 },
  { title: '生成类型', dataIndex: 'codeGenType', width: 140 },
  { title: '优先级', dataIndex: 'priority', width: 100 },
  { title: '部署时间', dataIndex: 'deployedTime', width: 180 },
  { title: '创建者', dataIndex: 'user', width: 160 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' },
]

/** 生成类型下拉：额外带一个「全部」 */
const codeGenTypeOptions = [{ label: '全部', value: '' }, ...CODE_GEN_TYPE_OPTIONS]

/** 列表数据 */
const dataList = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)

/** 搜索条件 */
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  appName: '',
  userId: '',
  codeGenType: '',
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
    const res = await appControllerListAppVoByPage({
      ...searchParams,
      // 空字符串不参与筛选
      appName: searchParams.appName || undefined,
      userId: searchParams.userId || undefined,
      codeGenType: searchParams.codeGenType || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取应用列表失败', error)
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
  searchParams.appName = ''
  searchParams.userId = ''
  searchParams.codeGenType = ''
  doSearch()
}

/** 表格换页 / 改变每页条数 */
const handleTableChange = (pag: { current?: number; pageSize?: number }) => {
  searchParams.pageNum = pag.current ?? 1
  searchParams.pageSize = pag.pageSize ?? 10
  fetchData()
}

/** 跳转到编辑页 */
const editApp = (record: API.AppVO) => {
  router.push(`/app/edit/${record.id}`)
}

/** 切换精选状态：99 为精选，0 为普通 */
const toggleFeatured = async (record: API.AppVO) => {
  if (!record.id) return
  const nextPriority = record.priority === FEATURED_PRIORITY ? 0 : FEATURED_PRIORITY
  try {
    const res = await appControllerUpdateAppByAdmin({
      id: record.id,
      priority: nextPriority,
    })
    if (res.data.code === 0) {
      message.success(nextPriority === FEATURED_PRIORITY ? '已设为精选' : '已取消精选')
      await fetchData()
    } else {
      message.error('操作失败，' + res.data.message)
    }
  } catch (error) {
    console.error('切换精选状态失败', error)
  }
}

/** 删除应用 */
const doDelete = async (id?: string) => {
  if (!id) return
  try {
    const res = await appControllerDeleteAppByAdmin({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      // 当前页删空且不是第一页时回退一页
      if (dataList.value.length === 1 && (searchParams.pageNum ?? 1) > 1) {
        searchParams.pageNum = (searchParams.pageNum ?? 1) - 1
      }
      await fetchData()
    } else {
      message.error('删除失败，' + res.data.message)
    }
  } catch (error) {
    console.error('删除应用失败', error)
  }
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

.prompt-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted-text {
  color: var(--ink-300);
}
</style>
