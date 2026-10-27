/** 会话（对应后端 P1 规划的 conversation 表，接口待接入） */
export interface Conversation {
  /** 会话 ID（后端生成） */
  id: string
  /** 会话标题，默认取首条用户消息 */
  title: string
  createdAt: number
  updatedAt: number
}

/** 创建会话请求 */
export interface CreateConversationRequest {
  title?: string
}

/** 更新会话请求 */
export interface UpdateConversationRequest {
  title: string
}
