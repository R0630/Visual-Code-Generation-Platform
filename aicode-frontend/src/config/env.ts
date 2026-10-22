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
 * 说明：后端返回的是完整 deployUrl，这里做兜底拼接
 */
export const getDeployUrl = (deployKeyOrUrl: string) => {
  if (/^https?:\/\//.test(deployKeyOrUrl)) {
    return deployKeyOrUrl
  }
  return `${DEPLOY_DOMAIN}/${deployKeyOrUrl}`
}

/**
 * 获取生成产物的预览地址（无需部署即可看效果）
 *
 * 后端静态资源：GET /api/static/{目录名}/  -> index.html
 * 注意：后端生成产物的目录名是 `{codeGenType}_{appId}`
 * （见 AppServiceImpl：sourceDirName = codeGenType + "_" + appId），
 * 部署目录 code_deploy/{deployKey} 目前没有对外提供静态访问，
 * 所以预览统一用 codeGenType + appId 拼接，否则会 404。
 */
export const getStaticPreviewUrl = (codeGenType?: string, appId?: string) => {
  if (!codeGenType || !appId) return ''
  return `${STATIC_BASE_URL}/${codeGenType}_${appId}/`
}
