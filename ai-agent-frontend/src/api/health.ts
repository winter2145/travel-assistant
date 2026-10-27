import http from './http'

/** 后端健康检查：GET /api/health（真实接口，返回纯文本 ok） */
export async function checkHealth(): Promise<string> {
  const res = await http.get<string>('/health')
  return res
}
