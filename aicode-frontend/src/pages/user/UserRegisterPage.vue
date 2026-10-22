<template>
  <div id="userRegisterPage">
    <div class="auth-card purple-card">
      <!-- 顶部插画 / Logo：替换 src/assets/logo.svg 与 illustrations 即可 -->
      <div class="auth-illustration illustration-wrap">
        <img src="@/assets/illustrations/hero-illustration.svg" alt="注册页插画" />
      </div>

      <h2 class="auth-title">创建你的账号</h2>
      <p class="auth-desc">30 秒完成注册，立即开始生成应用</p>

      <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
        <a-form-item
          name="userAccount"
          :rules="[
            { required: true, message: '请输入账号' },
            { min: 4, message: '账号不能小于 4 位' },
          ]"
        >
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
        <a-form-item
          name="checkPassword"
          :rules="[
            { required: true, message: '请确认密码' },
            { min: 8, message: '密码不能小于 8 位' },
            { validator: validateCheckPassword, trigger: 'change' },
          ]"
        >
          <a-input-password
            v-model:value="formState.checkPassword"
            placeholder="请确认密码"
            size="large"
          />
        </a-form-item>
        <div class="tips">
          已有账号？
          <RouterLink to="/user/login">去登录</RouterLink>
        </div>
        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" :loading="submitting" block>
            注册
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userControllerUserRegister } from '@/api/userController'

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const router = useRouter()
const submitting = ref(false)

/**
 * 校验两次输入的密码是否一致
 */
const validateCheckPassword = async (_rule: unknown, value: string) => {
  if (!value) {
    return Promise.reject('请确认密码')
  }
  if (value !== formState.userPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

/**
 * 提交表单
 */
const handleSubmit = async () => {
  if (submitting.value) return
  submitting.value = true
  try {
    const res = await userControllerUserRegister(formState)
    // 注册成功，跳转到登录页
    if (res.data.code === 0 && res.data.data) {
      message.success('注册成功，请登录')
      await router.push('/user/login')
    } else {
      message.error('注册失败，' + res.data.message)
    }
  } catch (error) {
    console.error('注册失败', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
#userRegisterPage {
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
</style>
