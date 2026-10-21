<template>
  <div class="home-page">
    <h1>首页</h1>
    <p>欢迎来到 AI Code</p>

    <a-space direction="vertical" style="margin-top: 24px">
      <a-button type="primary" :loading="loading" @click="testHealth"> 测试健康检查接口 </a-button>
      <div v-if="result !== null" class="result">
        <p><strong>接口返回：</strong>{{ result }}</p>
      </div>
    </a-space>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import api from '@/api/aicode'

const loading = ref(false)
const result = ref<string | null>(null)

const testHealth = async () => {
  loading.value = true
  result.value = null
  try {
    const data = await api.healthCheck.healthCheckHealth()
    result.value = data as unknown as string
  } catch (error) {
    result.value = `请求失败：${(error as Error).message}`
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.home-page {
  padding: 16px 0;
}

.result {
  margin-top: 12px;
  padding: 12px 16px;
  background: #f5f5f5;
  border-radius: 6px;
  font-family: monospace;
}
</style>
