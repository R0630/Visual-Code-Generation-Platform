// 根据后端 OpenAPI（Knife4j / springdoc）生成前端请求代码与 TS 模型
// 使用：确保后端已启动（http://localhost:8123），然后执行 npm run gen:api

export default {
  // 统一使用 src/request.ts（返回完整 BaseResponse，业务页面自行判断 code === 0）
  requestLibPath: "import request from '@/request'",
  schemaPath: 'http://localhost:8123/api/v3/api-docs',
  serversPath: './src',
}
