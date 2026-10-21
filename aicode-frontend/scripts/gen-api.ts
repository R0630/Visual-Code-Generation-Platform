import { generateService } from '@umijs/openapi'

generateService({
  schemaPath: 'http://localhost:8123/api/v3/api-docs',
  serversPath: './src/api',
  requestLibPath: "import request from '@/utils/request'",
  projectName: 'aicode',
  isCamelCase: true,
})
