/** 对话消息角色 */
export type ChatRole = 'user' | 'assistant'

/** 消息状态 */
export type MessageStatus = 'sending' | 'streaming' | 'done' | 'error'

/** 消息中的工具调用事件（对应后端 SSE 的 Step 输出） */
export interface ToolEvent {
  /** 事件类型：工具调用中 / 工具返回 */
  kind: 'tool_call' | 'tool_result'
  /** 工具名，如 searchAttractions */
  toolName?: string
  /** 工具参数 JSON 字符串 */
  args?: string
  /** 工具返回结果文本 */
  result?: string
  /** Agent 执行步数（Step N） */
  step?: number
}

/** 面向用户展示的规划进度，不包含工具参数或原始返回数据。 */
export interface ExecutionStep {
  id: string
  label: string
  state: 'active' | 'done' | 'error'
}

/** 单条聊天消息 */
export interface ChatMessage {
  /** 消息 ID（前端生成 UUID） */
  id: string
  role: ChatRole
  content: string
  status: MessageStatus
  /** 创建时间戳 ms */
  createdAt: number
  /** 消息关联的工具调用记录（仅 assistant 消息可能有） */
  toolEvents: ToolEvent[]
  executionSteps?: ExecutionStep[]
  /** 错误信息（status=error 时有值） */
  error?: string
}

/** SSE 事件类型（对应后端 /api/ai/manus/chat 流） */
export type SseEventType = 'step' | 'answer' | 'error' | 'done'

/** 解析后的 SSE 事件 */
export interface SseEvent {
  type: SseEventType
  /** 事件名称（后端可能给 event: answer） */
  eventName?: string
  /** 事件数据 */
  data: string
}

/** 对话请求参数（真实接口：GET /api/ai/manus/chat） */
export interface ChatRequest {
  message: string
}

/** 对话流式回调 */
export interface StreamChatHandlers {
  onEvent: (event: SseEvent) => void
  onDone: () => void
  onError: (error: Error) => void
}
