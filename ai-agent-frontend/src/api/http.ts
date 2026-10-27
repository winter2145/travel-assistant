import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResult } from '@/types/api'

/**
 * 统一 HTTP 客户端。
 * 响应拦截器已把 AxiosResponse 解包为业务数据，因此这里把 get/post 的类型声明为
 * 直接返回 Promise<T>（避免调用方拿到 AxiosResponse）。
 */
export interface HttpClient {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
}

/** Axios 实例：统一 baseURL、超时与响应解包。后端 context-path 为 /api，开发环境经 Vite 代理转发到 8123。 */
const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
}) as unknown as HttpClient & AxiosInstance

// 响应拦截：统一解包 ApiResult；非 success 抛出携带 message 的错误
http.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data as ApiResult
    // 后端当前仅 /health 返回纯文本；其余接口按 ApiResult 契约解析
    if (body && typeof body === 'object' && 'success' in body) {
      if (body.success) {
        return body.data as never
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response.data as never
  },
  (error) => {
    const status = error?.response?.status
    const responseBody = error?.response?.data as { message?: unknown } | undefined
    let message = '网络请求失败'
    if (typeof responseBody?.message === 'string' && responseBody.message) {
      message = responseBody.message
    } else if (status === 401 || status === 403) {
      message = '未授权或登录已过期'
    } else if (status === 404) {
      message = '接口不存在（后端尚未提供该能力）'
    } else if (status >= 500) {
      message = '服务器内部错误'
    } else if (error?.code === 'ECONNABORTED') {
      message = '请求超时，请检查后端服务是否已启动'
    } else if (error?.message) {
      message = error.message
    }
    return Promise.reject(new Error(message))
  },
)

export default http
