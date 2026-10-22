<template>
  <div id="userLoginPage">
    <div class="auth-card purple-card">
      <!-- 顶部插画 / Logo：替换 src/assets/logo.svg 与 illustrations 即可 -->
      <div class="auth-illustration illustration-wrap">
        <img src="@/assets/illustrations/hero-illustration.svg" alt="登录页插画" />
      </div>

      <h2 class="auth-title">欢迎回来!</h2>
      <p class="auth-desc">登录后即可创建并保存你的应用</p>

      <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
        <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input
            v-model:value="formState.userAccount"
            placeholder="请输入账号"
            size="large"
            allow-clear
          />
        </a-form-item>
        <a-form-item
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码不能小于 8 位' },
          ]"
        >
          <a-input-password
            v-model:value="formState.userPassword"
            placeholder="请输入密码"
            size="large"
          />
        </a-form-item>
        <div class="tips">
          没有账号？
          <RouterLink to="/user/register">去注册</RouterLink>
        </div>
        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" :loading="submitting" block>
            登录
          </a-button>
        </a-form-item>
      </a-form>

      <div class="demo-account">【测试账号提示：上线前可删除】admin001 / admin123</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userControllerUserLogin } from '@/api/userController'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const submitting = ref(false)

// 登录成功后需要回跳的地址
const redirectUrl = ref('/')

onMounted(() => {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/')) {
    redirectUrl.value = redirect
  }
})

/**
 * 提交表单
 */
const handleSubmit = async () => {
  if (submitting.value) return
  submitting.value = true
  try {
    const res = await userControllerUserLogin(formState)
    // 登录成功，把登录态保存到全局状态中
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      await router.replace(redirectUrl.value)
    } else {
      message.error('登录失败，' + res.data.message)
    }
  } catch (error) {
    console.error('登录失败', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
#userLoginPage {
  min-height: calc(100vh - 200px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background:
    radial-gradient(700px 300px at 20% 0%, rgba(167, 139, 250, 0.28), transparent 60%),
    radial-gradient(600px 280px at 85% 20%, rgba(196, 181, 253, 0.3), transparent 60%);
}

.auth-card {
  width: 100%;
  max-width: 420px;
  box-shadow: var(--shadow-md);
}

.auth-illustration img {
  width: 100%;
  max-width: 260px;
}

.auth-title {
  margin: 8px 0 6px;
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  color: var(--ink-900);
}

.auth-desc {
  margin: 0 0 20px;
  text-align: center;
  color: var(--ink-500);
  font-size: 14px;
}

.tips {
  margin-bottom: 16px;
  color: var(--ink-500);
  font-size: 13px;
  text-align: right;
}

.demo-account {
  margin-top: 8px;
  text-align: center;
  color: var(--ink-300);
  font-size: 12px;
}
</style>
