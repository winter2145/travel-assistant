import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { BudgetCheckResult, Itinerary } from '@/types/plan'
import { normalizeItinerary } from '@/utils/itinerary'

/**
 * 当前旅行计划状态（Pinia）。
 *
 * 数据来源（真实）：对话 SSE 流中工具返回的行程/预算 JSON（parseToolStep 解析）。
 * 后端行程持久化接口（P2）接入前，不落库；刷新页面后清空。
 */
export const usePlanStore = defineStore('plan', () => {
  const itinerary = ref<Itinerary | null>(null)
  const budget = ref<BudgetCheckResult | null>(null)
  const planId = ref<string | null>(null)
  const planVersion = ref(0)
  /** 数据来源标记：'live' 来自真实对话解析；'demo' 来自演示数据 */
  const source = ref<'live' | 'demo'>('live')

  const totalDays = computed(() => itinerary.value?.days.length ?? 0)
  const totalActivities = computed(() => {
    return itinerary.value?.days.reduce((sum, d) => sum + d.activities.length, 0) ?? 0
  })
  const activityCost = computed(() => itinerary.value?.estimatedActivityCost ?? 0)

  function setItinerary(value: Itinerary): void {
    itinerary.value = value
    source.value = 'live'
  }

  function setBudget(value: BudgetCheckResult): void {
    budget.value = value
  }

  function setSavedPlan(id: string, version: number): void {
    planId.value = id
    planVersion.value = version
  }

  function loadPlanRecord(value: { id: string; version: number; itinerary: Itinerary | null; budgetSummary?: BudgetCheckResult | null }): void {
    itinerary.value = value.itinerary ? normalizeItinerary(value.itinerary) : null
    budget.value = value.budgetSummary ?? null
    planId.value = value.id
    planVersion.value = value.version
    source.value = 'live'
  }

  /** 载入演示行程（仅布局开发用，界面须标注） */
  function loadDemo(demo: Itinerary): void {
    itinerary.value = demo
    source.value = 'demo'
  }

  function clear(): void {
    itinerary.value = null
    budget.value = null
    planId.value = null
    planVersion.value = 0
  }

  return {
    itinerary,
    budget,
    planId,
    planVersion,
    source,
    totalDays,
    totalActivities,
    activityCost,
    setItinerary,
    setBudget,
    setSavedPlan,
    loadPlanRecord,
    loadDemo,
    clear,
  }
})
