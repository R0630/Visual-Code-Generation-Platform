/**
 * 代码生成类型枚举（与后端 com.qiyiqi.aicode.model.enums.CodeGenTypeEnum 保持一致）
 */
export enum CodeGenTypeEnum {
  HTML = 'html',
  MULTI_FILE = 'multi_file',
  VUE_PROJECT = 'vue_project',
}

/**
 * 代码生成类型配置
 */
export const CODE_GEN_TYPE_CONFIG = {
  [CodeGenTypeEnum.HTML]: {
    label: '原生 HTML 模式',
    value: CodeGenTypeEnum.HTML,
    desc: '生成单个 HTML 文件，适合简单页面',
  },
  [CodeGenTypeEnum.MULTI_FILE]: {
    label: '原生多文件模式',
    value: CodeGenTypeEnum.MULTI_FILE,
    desc: '生成 HTML / CSS / JS 多文件，结构更清晰',
  },
  [CodeGenTypeEnum.VUE_PROJECT]: {
    label: 'Vue 项目模式',
    value: CodeGenTypeEnum.VUE_PROJECT,
    desc: '生成完整 Vue 项目，功能更丰富',
  },
} as const

/** 下拉选择用 */
export const CODE_GEN_TYPE_OPTIONS = Object.values(CODE_GEN_TYPE_CONFIG).map((config) => ({
  label: config.label,
  value: config.value,
}))

/** 类型值 -> 中文描述 */
export const formatCodeGenType = (type: string | undefined): string => {
  if (!type) return '未知类型'
  const config = CODE_GEN_TYPE_CONFIG[type as CodeGenTypeEnum]
  return config ? config.label : type
}
