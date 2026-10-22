import { defineStore } from 'pinia'
import { ref } from 'vue'
import { userControllerGetLoginUser } from '@/api/userController'

/** 未登录时的占位用户对象 */
export const DEFAULT_LOGIN_USER: API.LoginUserVO = {
  userName: '未登录',
}

export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVO>(DEFAULT_LOGIN_USER)

  /** 获取登录用户信息（未登录时保持默认占位对象） */
  async function fetchLoginUser() {
    try {
      const res = await userControllerGetLoginUser()
      const data = res.data
      if (data?.code === 0 && data.data) {
        loginUser.value = data.data
      } else {
        loginUser.value = DEFAULT_LOGIN_USER
      }
    } catch (error) {
      console.error('获取登录用户信息失败', error)
      loginUser.value = DEFAULT_LOGIN_USER
    }
  }

  /** 更新登录用户信息 */
  function setLoginUser(newLoginUser: API.LoginUserVO) {
    loginUser.value = newLoginUser ?? DEFAULT_LOGIN_USER
  }

  /** 清空登录态 */
  function clearLoginUser() {
    loginUser.value = DEFAULT_LOGIN_USER
  }

  return { loginUser, setLoginUser, fetchLoginUser, clearLoginUser }
})
