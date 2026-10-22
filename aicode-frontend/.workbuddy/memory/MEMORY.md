# aicode-frontend 项目长期笔记

## 架构约定

- 后端：`E:/code/aicode`（Spring Boot 3，端口 8123，context-path `/api`，MyBatis-Flex，Knife4j 文档 `http://localhost:8123/api/v3/api-docs`）。
- 前端：Vue3 + Vite + ant-design-vue 4 + Pinia，dev 端口 5173，vite 代理 `/api` → `http://localhost:8123`（保留 /api 前缀，无需 rewrite）。
- 请求层唯一入口 `src/request.ts`：返回完整 AxiosResponse，业务页判断 `res.data.code === 0`；错误码 40100=未登录、40101=无权限（对应后端 ErrorCode）。
- API 代码由 `npm run gen:api`（@umijs/openapi）生成到 `src/api`；requestLibPath 必须是 `@/request`。
- 后端实体 id/userId 均为雪花 ID 且用 ToStringSerializer 序列化成字符串 → 前端类型必须是 string；gen:api 重新生成后要手工把 typings.d.ts 里的 number 改回 string。
- 路由权限：`/admin/**` 仅 admin；全局守卫在 `src/access.ts`（vue-router v5 用返回值，不用 next()）；菜单由 BasicLayout 从 routes 生成，meta.hideInMenu 控制隐藏。
- 测试账号见 `E:/code/aicode/sql/test_data.sql`：admin001/admin123、user001/user1234。
- 视觉风格：紫色简约插画风。设计变量集中在 `src/styles/theme.css`，antd 主题 token 在 `App.vue` 的 `a-config-provider`（colorPrimary #7c3aed）。新增页面一律用 `.page-shell` / `.purple-card` / `.section-title` 等工具类，不要自行定义新色值。
- 应用预览地址：`/api/static/{codeGenType}_{appId}/`（产物目录名规则），**不要**用 deployKey。
- 对话页：`/app/chat/:id`，SSE `GET /app/chat/gen/code`，消息体 `{"d":"片段"}`，结束事件 `done`；另有 `business-error` 兜底事件。
- 对话历史（游标分页）：`GET /chatHistory/app/{appId}?pageSize=10&lastCreateTime=<ISO>`，**服务端按 createTime 降序返回**，前端 reverse 成升序；`lastCreateTime` 取本页最旧一条作为下一页游标（后端用 `<` 过滤）。管理端分页：`POST /chatHistory/admin/list/page/vo`。
- 网站预览门槛：app 有 ≥2 条对话记录（一轮问答）或已部署（deployedTime）才展示 iframe。
- 参考实现项目（功能结构参考）：`E:/code/yu-ai-code-mother-master/ai-code-mother-frontend`。

## 前端编码规则（用户需掌握的要点）

- 一个项目只保留一个 axios 封装；生成代码与手写代码共用同一 request。
- `<script setup lang="ts">` 中用到的东西必须 import（vue 的 ref/computed 也要显式导入）；模板里不能出现未定义变量。
- Vue SFC 的 template 只能有一个根级结构，`<template #bodyCell>` 这类具名插槽必须写在 `<a-table>` 内部。
- antd-vue 下拉菜单 a-dropdown 默认 hover 触发，写自动化测试时用 `trigger="click"` 更稳。
- 冒烟测试方案：本机无 playwright/agent-browser 时，可用 Edge/Chrome headless + `--remote-debugging-port` + Node 22 全局 WebSocket 走 CDP（Runtime.evaluate 填表单、点击、截图），脚本放 Temp 目录用完即删。登录步骤直接在页内 `fetch('/api/user/login', ...)` 比填表单稳。
- TS 开了 `noUncheckedIndexedAccess`：数组下标访问一律判为可能 undefined，用 `arr.at(-1)?.x` 或安全 getter 包一层。

## 后端本地启动注意事项

- 启动前必须 `export SERVER_PORT=8123`（bash）/ `$env:SERVER_PORT='8123'`（PowerShell），否则环境里泄漏的 SERVER_PORT 会让 Spring Boot 起在随机端口（如 56473）。
- bash 里 `mvn` 会报 `ClassNotFoundException: plexus.classworlds.launcher.Launcher`，改用 PowerShell 执行。
- 后台跑 `mvn spring-boot:run` 必须显式给长 timeout（如 600000），否则默认 2 分钟超时会把进程杀掉，表现为接口 502。
- 后端所有雪花 ID 字段都要加 `@JsonSerialize(using = ToStringSerializer.class)`（ChatHistory 曾漏加，已修）。
