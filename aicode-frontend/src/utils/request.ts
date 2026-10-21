import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import router from '@/router'

/** 后端统一响应结构 */
export interface ApiResponse<T = unknown> {
  code: number
  data: T
  message: string
}

/** 成功响应的 code，根据后端约定调整 */
const SUCCESS_CODE = 0

/** 未登录的 code，根据后端约定调整 */
const UNAUTHORIZED_CODE = 401

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  withCredentials: true,
})

/** 请求拦截器 */
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

/** 响应拦截器 */
service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse

    if (res.code === undefined) {
      return response.data
    }

    if (res.code === SUCCESS_CODE) {
      return res.data
    }

    if (res.code === UNAUTHORIZED_CODE) {
      handleUnauthorized()
      return Promise.reject(new Error(res.message || '未登录或登录已过期'))
    }

    message.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      handleUnauthorized()
    } else {
      message.error(error.response?.data?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  },
)

/** 未登录处理：跳转登录页 */
function handleUnauthorized() {
  message.warning('登录已过期，请重新登录')
  const currentPath = router.currentRoute.value.fullPath
  router.push({
    path: '/login',
    query: { redirect: currentPath },
  })
}

/** 封装请求方法 */
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service.request(config) as unknown as Promise<T>
}

export default service
