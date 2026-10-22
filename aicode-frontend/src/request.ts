import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'

/**
 * 统一请求层
 * 说明：
 * - 这里的 axios 实例不解析业务状态码，统一返回完整的响应对象（AxiosResponse），
 *   业务页面通过 res.data.code === 0 判断成功，和后端 BaseResponse{code,data,message} 一一对应。
 * - 拦截器只处理两类「非业务」问题：登录态失效（40100 / 40101）和网络异常。
 */

// 创建 Axios 实例
const myAxios = axios.create({
  // 开发环境走 vite 代理（见 vite.config.ts），生产环境可用 VITE_API_BASE_URL 覆盖
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60000,
  // 允许携带 Cookie，后端 Session 才能生效
  withCredentials: true,
})

// 后端错误码（与 com.qiyiqi.aicode.exception.ErrorCode 保持一致）
const NOT_LOGIN_CODE = 40100
const NO_AUTH_CODE = 40101

/** 是否已经在处理登录态失效，避免同一时刻弹多次提示 */
let loginExpiredHandling = false

function isGetLoginUserRequest(url = '') {
  return url.includes('/user/get/login')
}

function handleLoginExpired(currentUrl = '') {
  // 已经在登录页就不要重复跳转
  if (window.location.pathname.startsWith('/user/login')) {
    return
  }
  if (loginExpiredHandling) {
    return
  }
  loginExpiredHandling = true
  message.warning('请先登录')
  window.setTimeout(() => {
    loginExpiredHandling = false
  }, 1000)
  window.location.href = `/user/login?redirect=${encodeURIComponent(currentUrl || window.location.href)}`
}

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config: InternalAxiosRequestConfig) {
    return config
  },
  function (error) {
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response: AxiosResponse) {
    const data = response.data
    // 后端统一返回结构 BaseResponse { code, data, message }
    if (data && typeof data === 'object' && 'code' in data) {
      const code = data.code as number
      if (code === NOT_LOGIN_CODE || code === NO_AUTH_CODE) {
        // 获取登录用户接口只用于探测登录态，本身未登录是正常的，不跳转
        if (!isGetLoginUserRequest(response.config.url)) {
          handleLoginExpired(window.location.href)
        }
      }
    }
    return response
  },
  function (error) {
    const status = error?.response?.status
    const serverMessage = error?.response?.data?.message
    if (status === 401 || status === 403) {
      handleLoginExpired(window.location.href)
    } else if (serverMessage) {
      message.error(serverMessage)
    } else if (error?.code === 'ECONNABORTED') {
      message.error('请求超时，请稍后重试')
    } else {
      message.error('网络异常，请确认后端服务（localhost:8123）已启动')
    }
    return Promise.reject(error)
  },
)

export default myAxios
