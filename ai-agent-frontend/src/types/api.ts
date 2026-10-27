/** 后端统一响应包装（对应后端工具契约 ToolResult，HTTP 层沿用同一风格） */
export interface ApiResult<T = unknown> {
  success: boolean
  code: string
  message: string
  data: T
}

/** 分页请求基础参数 */
export interface PageParams {
  page: number
  pageSize: number
}

/** 分页响应 */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}
