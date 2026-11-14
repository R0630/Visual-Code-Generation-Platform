/**
 * 环境变量与外链配置
 * 说明：默认全部走 Vite 代理（/api -> http://localhost:8123），部署时可在 .env 里覆盖
 */


// API 前缀（与后端 context-path /api 一致）
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// 部署后的应用访问域名（部署成功弹窗用）
export const DEPLOY_DOMAIN = import.meta.env.VITE_DEPLOY_DOMAIN || ''

// 静态资源（生成产物预览）前缀
export const STATIC_BASE_URL = `${API_BASE_URL}/static`

/**
 * 获取已部署应用的访问地址
 * - 如果后端 AppVO 直接给了完整 URL（http/https 开头），直接使用
 * - 否则拼接为：
 *   · 有 DEPLOY_DOMAIN：{DEPLOY_DOMAIN}/xinggou/{key}/ （生产环境，简洁专业）
 *   · 无 DEPLOY_DOMAIN：/xinggou/{key}/ （本地开发，走 Vite 代理到后端）
 */
export const getDeployUrl = (deployKeyOrUrl: string) => {
  if (!deployKeyOrUrl) return ''
  if (/^https?:\/\//.test(deployKeyOrUrl)) {
    return deployKeyOrUrl
  }
  const path = `/xinggou/${deployKeyOrUrl}/`
  if (DEPLOY_DOMAIN) {
    return `${DEPLOY_DOMAIN}${path}`
  }
  return path
}

/**
 * 获取生成产物的预览地址（无需部署即可看效果）
 *
 * 后端静态资源：GET /api/static/{目录名}/  -> index.html
 * 目录名是 `{codeGenType}_{appId}`
 * 对于 VUE_PROJECT，StaticResourceController 会自动优先返回 dist/index.html，
 * 前端直接访问目录即可，无需手动加 dist/index.html（否则会导致资源路径嵌套错误）
 */
export const getStaticPreviewUrl = (codeGenType: string, appId: string) => {
  return `${STATIC_BASE_URL}/${codeGenType}_${appId}/`
}
