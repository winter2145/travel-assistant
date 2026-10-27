import http from './http'
import type { BudgetCheckResult, Itinerary, TravelPlan } from '@/types/plan'

/** 保存行程：POST /api/plans */
export async function savePlan(
  req: {
    conversationId: string
    title: string
    itinerary: Itinerary
    budgetSummary?: BudgetCheckResult | null
  },
): Promise<TravelPlan> {
  return http.post<TravelPlan>('/plans', req)
}

/** 行程列表：GET /api/plans */
export async function listPlans(params: { page: number; pageSize: number; keyword?: string }): Promise<{
  list: TravelPlan[]
  total: number
}> {
  return http.get('/plans', { params })
}

/** 行程详情：GET /api/plans/:id */
export async function getPlan(id: string): Promise<TravelPlan> {
  return http.get<TravelPlan>(`/plans/${id}`)
}

export async function getPlanByConversation(conversationId: string): Promise<TravelPlan> {
  return http.get<TravelPlan>(`/plans/by-conversation/${conversationId}`)
}

/** 更新行程：PUT /api/plans/:id（前端编辑后保存） */
export async function updatePlan(id: string, req: {
  title?: string
  itinerary: Itinerary
  budgetSummary?: BudgetCheckResult | null
  version: number
}): Promise<TravelPlan> {
  return http.put<TravelPlan>(`/plans/${id}`, req)
}

/** 删除行程：DELETE /api/plans/:id */
export async function deletePlan(id: string): Promise<void> {
  return http.delete(`/plans/${id}`)
}

/** 复制行程：POST /api/plans/:id/duplicate */
export async function duplicatePlan(id: string): Promise<TravelPlan> {
  return http.post<TravelPlan>(`/plans/${id}/duplicate`)
}
