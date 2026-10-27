import type { SseEvent, SseEventType } from '@/types/chat'

/**
 * 逐行解析 SSE 文本块，返回解析出的完整事件。
 * 兼容后端 /api/ai/manus/chat 的输出：
 *   data:Step 1: ...（无 event 名）
 *   event:answer\ndata:...（带 event 名）
 */
export function parseSseChunk(chunk: string): SseEvent[] {
  const events: SseEvent[] = []
  // 按空行切分事件块（SSE 规范：事件以空行分隔）
  const blocks = chunk.split(/\r?\n\r?\n/)
  for (const block of blocks) {
    const lines = block.split(/\r?\n/)
    const dataLines: string[] = []
    let eventName = ''
    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trimStart())
      } else if (line.startsWith(':')) {
        // 注释行忽略
        continue
      }
      // 忽略其他行
    }
    if (dataLines.length === 0) continue
    const data = dataLines.join('\n')
    const type: SseEventType =
      eventName === 'answer' ? 'answer' : eventName === 'done' ? 'done' : eventName === 'error' || data.startsWith('错误') || data.startsWith('执行错误') ? 'error' : 'step'
    events.push({ type, eventName: eventName || undefined, data })
  }
  return events
}

/**
 * 通过 fetch 流式读取 SSE 响应并逐事件回调。
 * 使用 fetch + AbortController 而非 EventSource：
 * 1) 可主动中断（为"停止生成"预留前端能力）；
 * 2) 便于后续切换到 POST /api/ai/chat 时复用同一解析逻辑。
 */
export async function streamSse(
  url: string,
  handlers: { onEvent: (e: SseEvent) => void; onDone: () => void; onError: (e: Error) => void },
  signal?: AbortSignal,
  request: { method?: 'GET' | 'POST'; body?: string } = {},
): Promise<void> {
  let response: Response
  try {
    response = await fetch(url, {
      method: request.method ?? 'GET',
      headers: {
        Accept: 'text/event-stream',
        ...(request.body ? { 'Content-Type': 'application/json' } : {}),
      },
      body: request.body,
      signal,
    })
  } catch (e) {
    handlers.onError(e instanceof Error ? e : new Error(String(e)))
    return
  }

  if (!response.ok || !response.body) {
    handlers.onError(new Error(`对话接口请求失败：HTTP ${response.status}`))
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // SSE 的 event 与 data 通常占多行，必须完整读取到空行再解析。
      let boundary: RegExpMatchArray | null
      while ((boundary = buffer.match(/\r?\n\r?\n/))) {
        const index = boundary.index ?? -1
        if (index < 0) break
        const block = buffer.slice(0, index + boundary[0].length)
        buffer = buffer.slice(index + boundary[0].length)
        for (const event of parseSseChunk(block)) handlers.onEvent(event)
      }
    }
    // 处理剩余缓冲
    if (buffer.trim().length > 0) {
      for (const event of parseSseChunk(buffer)) {
        handlers.onEvent(event)
      }
    }
    handlers.onDone()
  } catch (e) {
    // AbortError 由调用方按"用户主动停止"处理，不视为错误
    if (e instanceof DOMException && e.name === 'AbortError') {
      handlers.onDone()
    } else {
      handlers.onError(e instanceof Error ? e : new Error(String(e)))
    }
  }
}
