<template>
  <div id="userManagePage" class="page-shell">
    <a-flex justify="space-between" align="center" class="page-header">
      <h2 class="page-title">用户管理</h2>
      <a-space>
        <a-button type="primary" @click="openCreateModal">
          <template #icon>
            <PlusOutlined />
          </template>
          新增用户
        </a-button>
        <a-button @click="fetchData">
          <template #icon>
            <ReloadOutlined />
          </template>
          刷新
        </a-button>
      </a-space>
    </a-flex>

    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" class="search-form" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" allow-clear />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" allow-clear />
      </a-form-item>
      <a-form-item label="角色">
        <a-select
          v-model:value="searchParams.userRole"
          placeholder="全部角色"
          allow-clear
          style="width: 140px"
          :options="roleOptions"
        />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" html-type="submit">搜索</a-button>
          <a-button @click="doReset">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-divider />

    <!-- 用户表格 -->
    <a-table
      :columns="columns"
      :data-source="dataList"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image v-if="record.userAvatar" :src="record.userAvatar" :width="48" />
          <span v-else class="muted">暂无头像</span>
        </template>
        <template v-else-if="column.dataIndex === 'userProfile'">
          <span class="muted">{{ record.userProfile || '这个用户很懒，什么都没写' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <a-tag :color="record.userRole === 'admin' ? 'green' : 'blue'">
            {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
            <a-popconfirm
              title="确认删除该用户？"
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

    <!-- 新增 / 编辑用户弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingUser ? '编辑用户' : '新增用户'"
      :confirm-loading="submitting"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleModalOk"
    >
      <a-form :model="formState" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item v-if="!editingUser" label="账号" required>
          <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model:value="formState.userName" placeholder="请输入用户昵称" />
        </a-form-item>
        <a-form-item label="头像">
          <a-input v-model:value="formState.userAvatar" placeholder="请输入头像图片地址" />
        </a-form-item>
        <a-form-item label="简介">
          <a-textarea v-model:value="formState.userProfile" :rows="3" placeholder="请输入用户简介" />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="formState.userRole" :options="roleOptions" />
        </a-form-item>
        <a-alert
          v-if="!editingUser"
          type="info"
          show-icon
          message="新增用户默认密码为 12345678，账号创建后不可修改。"
        />
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  userControllerAddUser,
  userControllerDeleteUser,
  userControllerListUserVoByPage,
  userControllerUpdateUser,
} from '@/api/userController'

/** 表格列定义 */
const columns = [
  { title: 'ID', dataIndex: 'id', width: 180 },
  { title: '账号', dataIndex: 'userAccount', width: 140 },
  { title: '用户名', dataIndex: 'userName', width: 140 },
  { title: '头像', dataIndex: 'userAvatar', width: 100 },
  { title: '简介', dataIndex: 'userProfile' },
  { title: '角色', dataIndex: 'userRole', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 140 },
]

const roleOptions = [
  { label: '普通用户', value: 'user' },
  { label: '管理员', value: 'admin' },
]

/** 列表数据 */
const dataList = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)

/** 搜索条件 */
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  userAccount: '',
  userName: '',
  userRole: undefined,
})

/** 分页配置 */
const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (count: number) => `共 ${count} 条`,
}))

/** 格式化时间 */
const formatTime = (value?: string) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')

/** 获取列表数据 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await userControllerListUserVoByPage({
      ...searchParams,
      userAccount: searchParams.userAccount || undefined,
      userName: searchParams.userName || undefined,
      userRole: searchParams.userRole || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取用户列表失败', error)
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
  searchParams.userAccount = ''
  searchParams.userName = ''
  searchParams.userRole = undefined
  doSearch()
}

/** 表格换页 / 改变每页条数 */
const handleTableChange = (pag: { current?: number; pageSize?: number }) => {
  searchParams.pageNum = pag.current ?? 1
  searchParams.pageSize = pag.pageSize ?? 10
  fetchData()
}

/** 删除用户 */
const doDelete = async (id?: string) => {
  if (!id) return
  try {
    const res = await userControllerDeleteUser({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      // 如果当前页已删空且不是第一页，回退一页
      if (dataList.value.length === 1 && (searchParams.pageNum ?? 1) > 1) {
        searchParams.pageNum = (searchParams.pageNum ?? 1) - 1
      }
      await fetchData()
    } else {
      message.error('删除失败，' + res.data.message)
    }
  } catch (error) {
    console.error('删除用户失败', error)
  }
}

/** 弹窗表单 */
const modalVisible = ref(false)
const submitting = ref(false)
const editingUser = ref<API.UserVO | null>(null)

const createEmptyForm = (): API.UserAddRequest & { id?: string } => ({
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

const formState = reactive<API.UserAddRequest & { id?: string }>(createEmptyForm())

const openCreateModal = () => {
  editingUser.value = null
  Object.assign(formState, createEmptyForm())
  modalVisible.value = true
}

const openEditModal = (record: API.UserVO) => {
  editingUser.value = record
  Object.assign(formState, createEmptyForm(), {
    id: record.id,
    userAccount: record.userAccount,
    userName: record.userName,
    userAvatar: record.userAvatar,
    userProfile: record.userProfile,
    userRole: record.userRole ?? 'user',
  })
  modalVisible.value = true
}

const handleModalOk = async () => {
  submitting.value = true
  try {
    if (editingUser.value) {
      // 编辑：baseURL /user/update
      const res = await userControllerUpdateUser({
        id: formState.id,
        userName: formState.userName,
        userAvatar: formState.userAvatar,
        userProfile: formState.userProfile,
        userRole: formState.userRole,
      })
      if (res.data.code === 0) {
        message.success('修改成功')
        modalVisible.value = false
        await fetchData()
      } else {
        message.error('修改失败，' + res.data.message)
      }
    } else {
      // 新增：BaseResponse /user/add
      if (!formState.userAccount) {
        message.warning('请输入账号')
        return
      }
      const res = await userControllerAddUser({
        userAccount: formState.userAccount,
        userName: formState.userName,
        userAvatar: formState.userAvatar,
        userProfile: formState.userProfile,
        userRole: formState.userRole,
      })
      if (res.data.code === 0) {
        message.success('新增成功，默认密码 12345678')
        modalVisible.value = false
        await fetchData()
      } else {
        message.error('新增失败，' + res.data.message)
      }
    }
  } catch (error) {
    console.error('保存用户失败', error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#userManagePage {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}

.page-header {
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.search-form {
  margin-bottom: 8px;
}

.search-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}

.muted {
  color: rgba(0, 0, 0, 0.45);
}
</style>
