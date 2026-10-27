import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { ChatMessage, SseEvent } from '@/types/chat'
import { streamSessionChat } from '@/api/chat'
import { listMessages } from '@/api/conversation'
import { savePlan, updatePlan } from '@/api/plan'
import { parseToolStep } from '@/utils/itinerary'
import { usePlanStore } from './plan'
import { useConversationStore } from './conversation'
import { ElMessage } from 'element-plus'

/** 生成前端消息 ID */
export function genMessageId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `msg-${Date.now()}-${Math.floor(Math.random() * 1e6)}`
}

/**
 * 对话状态（Pinia）。
 *
 * 数据存放原则：聊天记录不保存在组件局部状态；后端会话接口（P1）接入前，
 * 当前会话消息存于 Pinia（内存态），刷新页面后清空 —— 与"会话列表待接入"状态一致。
 */
export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])
  const isStreaming = ref(false)
  const lastError = ref<Error | null>(null)

  // 当前流式请求的 AbortController（为"停止生成"预留；后端 P5 将提供服务端中断）
  let abortController: AbortController | null = null

  const lastAssistantMessage = computed(() => {
    for (let i = messages.value.length - 1; i >= 0; i--) {
      if (messages.value[i].role === 'assistant') return messages.value[i]
    }
    return null
  })

  /** 追加用户消息并立即返回其 ID */
  function pushUserMessage(content: string): string {
    const msg: ChatMessage = {
      id: genMessageId(),
      role: 'user',
      content,
      status: 'done',
      createdAt: Date.now(),
      toolEvents: [],
    }
    messages.value.push(msg)
    return msg.id
  }

  /** 追加占位 assistant 消息（流式中） */
  function pushAssistantPlaceholder(): ChatMessage {
    const msg: ChatMessage = {
      id: genMessageId(),
      role: 'assistant',
      content: '',
      status: 'streaming',
      createdAt: Date.now(),
      toolEvents: [],
      executionSteps: [{ id: 'understand', label: '正在梳理旅行需求', state: 'active' }],
    }
    messages.value.push(msg)
    // ref() deep-wraps objects when accessed through the array. Return that proxy,
    // not the raw object passed to push, so asynchronous SSE mutations stay reactive.
    return messages.value[messages.value.length - 1]
  }

  /**
   * 发送消息并流式接收回答（真实接口：GET /api/ai/manus/chat）。
   * Step 文本会解析出工具调用事件与结构化行程/预算数据，同步到 plan store。
   */
  async function sendMessage(content: string): Promise<void> {
    const trimmed = content.trim()
    if (!trimmed || isStreaming.value) return

    lastError.value = null
    isStreaming.value = true
    const conversations = useConversationStore()
    let conversationId: string
    try {
      conversationId = await conversations.ensureActiveConversation()
    } catch {
      isStreaming.value = false
      lastError.value = new Error('暂时无法开启旅行会话，请稍后重试。')
      return
    }
    pushUserMessage(trimmed)
    const assistant = pushAssistantPlaceholder()

    abortController = new AbortController()

    const planStore = usePlanStore()

    streamSessionChat(
      conversationId,
      trimmed,
      {
        onEvent: (event: SseEvent) => {
          if (event.type === 'done') {
            finishAssistant()
          } else if (event.type === 'answer') {
            assistant.content = event.data
          } else if (event.type === 'step') {
            // 后端 Step 中含工具名、参数和原始返回值，仅用于本地解析，不直接展示给用户。
            const payload = parseToolStep(event.data)
            if (payload.toolName) {
              const label = getFriendlyToolLabel(payload.toolName)
              const steps = assistant.executionSteps ?? []
              steps.forEach((step) => { step.state = 'done' })
              if (!steps.some((step) => step.id === payload.toolName)) {
                steps.push({ id: payload.toolName, label, state: 'done' })
              }
              setActiveStep(steps, 'organize', '正在整理行程建议')
              assistant.executionSteps = steps
            } else if (event.data.includes('思考完成')) {
              const steps = assistant.executionSteps ?? []
              setActiveStep(steps, 'organize', '正在整理行程建议')
              assistant.executionSteps = steps
            }
            if (payload.itinerary) {
              planStore.setItinerary(payload.itinerary)
            }
            if (payload.budget) {
              planStore.setBudget(payload.budget)
            }
          } else if (event.type === 'error') {
            lastError.value = new Error('规划过程中遇到问题，请稍后重试。')
          }
        },
        onDone: () => {
          finishAssistant()
        },
        onError: (_error) => {
          assistant.executionSteps?.forEach((step) => {
            if (step.state === 'active') step.state = 'error'
          })
          assistant.status = 'error'
          const friendlyError = new Error('暂时连接不上旅行规划服务，请稍后再试。')
          assistant.error = friendlyError.message
          lastError.value = friendlyError
          isStreaming.value = false
          abortController = null
        },
      },
      abortController.signal,
    )

    function finishAssistant() {
      if (assistant.status !== 'streaming') return
          assistant.executionSteps?.forEach((step) => {
            if (step.state === 'active') step.state = lastError.value ? 'error' : 'done'
          })
          assistant.status = lastError.value ? 'error' : 'done'
          if (lastError.value) {
            assistant.error = lastError.value.message
          }
          // Mark the assistant complete as soon as the SSE stream closes. Keep the
          // send lock until persistence finishes so the sidebar cannot race the save.
          abortController = null
          if (!lastError.value) {
            void Promise.all([conversations.refresh(), persistCurrentPlan(conversationId)])
              .finally(() => { isStreaming.value = false })
          } else {
            isStreaming.value = false
          }
    }
  }

  async function loadMessages(conversationId: string): Promise<void> {
    const result = await listMessages(conversationId, { page: 1, pageSize: 100 })
    messages.value = result.list.map((message) => ({
      ...message,
      status: 'done' as const,
      toolEvents: [],
    }))
    lastError.value = null
  }

  async function persistCurrentPlan(conversationId: string): Promise<void> {
    const planStore = usePlanStore()
    const itinerary = planStore.itinerary
    if (!itinerary || planStore.source !== 'live') return
    try {
      const payload = {
        conversationId,
        title: `${itinerary.city}旅行计划`,
        itinerary,
        budgetSummary: planStore.budget,
      }
      const saved = planStore.planId
        ? await updatePlan(planStore.planId, { ...payload, version: planStore.planVersion })
        : await savePlan(payload)
      planStore.setSavedPlan(saved.id, saved.version)
      await useConversationStore().refresh()
    } catch {
      ElMessage.warning('行程已生成，但暂时没有保存成功；你可以稍后在行程页重试保存。')
    }
  }

  function setActiveStep(steps: NonNullable<ChatMessage['executionSteps']>, id: string, label: string) {
    steps.forEach((step) => { step.state = 'done' })
    const current = steps.find((step) => step.id === id)
    if (current) current.state = 'active'
    else steps.push({ id, label, state: 'active' })
  }

  function getFriendlyToolLabel(toolName: string): string {
    const name = toolName.toLowerCase()
    if (name.includes('attraction') || name.includes('place') || name.includes('sight')) return '查询景点与活动'
    if (name.includes('route') || name.includes('direction') || name.includes('traffic')) return '核对交通路线'
    if (name.includes('weather')) return '查看目的地天气'
    if (name.includes('hotel') || name.includes('stay')) return '查找住宿选择'
    if (name.includes('image') || name.includes('photo') || name.includes('pexels')) return '整理目的地图片'
    return '查询旅行相关信息'
  }

  /** 停止当前生成（前端中断 SSE 连接；后端服务端中断待 P5） */
  function stopStreaming(): void {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
  }

  /** 重新生成：移除最后一条 assistant 消息，重发最后一条用户消息 */
  async function regenerate(): Promise<void> {
    const lastUser = [...messages.value].reverse().find((m) => m.role === 'user')
    if (!lastUser || isStreaming.value) return
    // 删除其后的所有消息
    const idx = messages.value.indexOf(lastUser)
    messages.value = messages.value.slice(0, idx)
    await sendMessage(lastUser.content)
  }

  /** 清空当前会话（新建旅行） */
  function resetSession(): void {
    stopStreaming()
    messages.value = []
    lastError.value = null
  }

  return {
    messages,
    isStreaming,
    lastError,
    lastAssistantMessage,
    sendMessage,
    loadMessages,
    stopStreaming,
    regenerate,
    resetSession,
  }
})
