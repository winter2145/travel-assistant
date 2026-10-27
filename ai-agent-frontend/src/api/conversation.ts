import http from './http'
import type { Conversation, CreateConversationRequest, UpdateConversationRequest } from '@/types/conversation'
import type { ChatMessage } from '@/types/chat'
import type { PageParams, PageResult } from '@/types/api'
import { streamSessionChat as streamChat } from './chat'

/** 创建会话：POST /api/conversations */
export async function createConversation(req: CreateConversationRequest): Promise<Conversation> {
  return http.post<Conversation>('/conversations', req)
}

/** 会话列表：GET /api/conversations */
export async function listConversations(params: PageParams): Promise<PageResult<Conversation>> {
  return http.get<PageResult<Conversation>>('/conversations', { params })
}

/** 删除会话：DELETE /api/conversations/:id */
export async function deleteConversation(id: string): Promise<void> {
  return http.delete(`/conversations/${id}`)
}

/** 更新会话：PATCH /api/conversations/:id */
export async function updateConversation(id: string, req: UpdateConversationRequest): Promise<Conversation> {
  return http.patch<Conversation>(`/conversations/${id}`, req)
}

/** 历史消息：GET /api/conversations/:id/messages */
export async function listMessages(
  conversationId: string,
  params: PageParams,
): Promise<PageResult<ChatMessage>> {
  const result = await http.get<PageResult<{ id: string; role: 'user' | 'assistant'; content: string; createdAt: number }>>(
    `/conversations/${conversationId}/messages`, { params },
  )
  return {
    ...result,
    list: result.list.map((message) => ({ ...message, status: 'done', toolEvents: [] })),
  }
}

/**
 * 带会话 ID 的流式对话：POST /api/ai/chat（SSE）
 * P0 计划新增；接入后替换 streamManusChat，并支持后端中断。
 */
export async function streamSessionChat(
  conversationId: string,
  message: string,
  handlers: { onEvent: (e: import('@/types/chat').SseEvent) => void; onDone: () => void; onError: (e: Error) => void },
  signal?: AbortSignal,
): Promise<void> {
  streamChat(conversationId, message, handlers, signal)
}
