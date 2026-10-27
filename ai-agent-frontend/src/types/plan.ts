/**
 * 行程相关类型。
 * 结构与后端行程工具（ItineraryGenerateTool / ItineraryAdjustTool）的 JSON 契约保持一致：
 * days[].activities[].{name, startTime, endTime, estimatedCost, travelMinutesFromPrevious}
 */

/** 单个行程活动 */
export interface ItineraryActivity {
  name: string
  location?: string
  /** 开始时间 HH:mm */
  startTime: string
  /** 结束时间 HH:mm */
  endTime: string
  /** 预估费用（元） */
  estimatedCost: number
  /** 与前一个活动的通勤分钟数 */
  travelMinutesFromPrevious?: number
  notes?: string
  /** Pexels MCP 在生成行程时查询并随行程保存的景点图片信息 */
  photoUrl?: string
  photoPageUrl?: string
  photoPhotographer?: string
  photoSource?: string
  photoSearchError?: string
}

/** 单日行程 */
export interface ItineraryDay {
  /** 第几天，从 1 开始 */
  day: number
  /** 日期 yyyy-MM-dd */
  date: string
  activities: ItineraryActivity[]
}

/** 结构化行程（对应后端 generateItinerary 返回） */
export interface Itinerary {
  city: string
  startDate: string
  days: ItineraryDay[]
  budget: number
  pace: string
  estimatedActivityCost: number
  notes?: string[]
}

/** 预算检查结果（对应后端 checkBudget 返回结构） */
export interface BudgetCheckResult {
  budget: number
  knownActivityCosts: number
  additionalCosts: number
  estimatedTotal: number
  remainingBudget: number
  overBudget: boolean
  activitiesWithCost: number
  suggestions: string[]
  warning?: string
}

/** 旅行计划（对应后端 P2 规划的 travel_plan 表，接口待接入） */
export interface TravelPlan {
  /** 计划 ID（后端生成） */
  id: string
  conversationId: string
  title: string
  city: string
  startDate: string
  days: number
  budget: number
  pace: string
  itinerary: Itinerary | null
  budgetSummary?: BudgetCheckResult | null
  createdAt: number
  updatedAt: number
  version: number
}
