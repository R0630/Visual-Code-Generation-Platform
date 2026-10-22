import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import router from '@/router'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 * vue-router v5 推荐直接返回结果，不再使用 next() 回调
 */
router.beforeEach(async (to) => {
  const loginUserStore = useLoginUserStore()
  // 确保页面刷新、首次加载时，能够等后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    firstFetchLoginUser = false
  }
  const loginUser = loginUserStore.loginUser

  // 需要登录才能访问的页面（如应用对话 / 编辑）
  if (to.meta.requireLogin && !loginUser?.id) {
    message.warning('请先登录')
    return `/user/login?redirect=${encodeURIComponent(to.fullPath)}`
  }

  // 管理后台：仅管理员
  if (to.fullPath.startsWith('/admin')) {
    if (!loginUser?.id) {
      message.warning('请先登录')
      return `/user/login?redirect=${encodeURIComponent(to.fullPath)}`
    }
    if (loginUser.userRole !== 'admin') {
      message.error('没有权限，仅管理员可访问')
      return '/'
    }
  }
  // 放行
  return true
})
