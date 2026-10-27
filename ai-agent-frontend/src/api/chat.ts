import { streamSse } from '@/utils/sse'
import type { SseEvent, StreamChatHandlers } from '@/types/chat'

/**
 * AI 对话接口。
 *
 * 【真实接口（已接入）】GET /api/ai/manus/chat?message=<urlencoded>
 * 后端返回 SSE：data:Step N: ... + event:answer + data:最终答案
 *
 * 说明：GET 长文本受限，P0 计划升级为 POST /api/ai/chat（见 conversation 契约）；
 * 此处保留真实 GET 调用，streamSse 已按可切换 POST 的方式编写。
 */
export function streamManusChat(
  message: string,
  handlers: StreamChatHandlers,
  signal?: AbortSignal,
): void {
  const url = `/api/ai/manus/chat?message=${encodeURIComponent(message)}`
  void streamSse(
    url,
    {
      onEvent: (e: SseEvent) => handlers.onEvent(e),
      onDone: () => handlers.onDone(),
      onError: (e: Error) => handlers.onError(e),
    },
    signal,
  )
}

/** Session-aware POST SSE endpoint; message text is sent in JSON, not a query string. */
export function streamSessionChat(
  conversationId: string,
  message: string,
  handlers: StreamChatHandlers,
  signal?: AbortSignal,
): void {
  void streamSse('/api/ai/manus/chat', {
    onEvent: handlers.onEvent,
    onDone: handlers.onDone,
    onError: handlers.onError,
  }, signal, { method: 'POST', body: JSON.stringify({ conversationId, message }) })
}
