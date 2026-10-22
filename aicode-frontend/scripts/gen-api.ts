import { generateService } from '@umijs/openapi'

// 根据后端 http://localhost:8123/api/v3/api-docs 生成 src/api 下的请求代码
// 注意：requestLibPath 必须指向 src/request.ts，保证全项目只有一套请求封装
generateService({
  schemaPath: 'http://localhost:8123/api/v3/api-docs',
  serversPath: './src',
  requestLibPath: "import request from '@/request'",
  projectName: 'aicode',
  isCamelCase: true,
})
