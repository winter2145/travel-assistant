<template>
  <div class="budget-summary" :class="statusClass">
    <div class="bs-grid">
      <div class="bs-item">
        <span class="bs-label">总预算</span>
        <b>¥{{ fmt(budget) }}</b>
      </div>
      <div class="bs-item">
        <span class="bs-label">活动费用</span>
        <b>¥{{ fmt(activityCost) }}</b>
      </div>
      <div class="bs-item">
        <span class="bs-label">其他费用</span>
        <b>¥{{ fmt(additionalCosts) }}</b>
      </div>
      <div class="bs-item" :class="{ 'is-over': overBudget }">
        <span class="bs-label">余额</span>
        <b class="bs-balance">{{ overBudget ? '超支 ¥' + fmt(Math.abs(remaining)) : '¥' + fmt(remaining) }}</b>
      </div>
    </div>

    <!-- 预算条形示意（确定性比例，非真实货币图表） -->
    <div class="bs-bar">
      <div class="bs-bar-fill" :style="{ width: barPercent + '%' }"></div>
    </div>
    <div class="bs-bar-caption">已用 {{ fmt(used) }} / {{ fmt(budget) }}（{{ barPercent.toFixed(1) }}%）</div>

    <ul v-if="suggestions.length > 0" class="bs-tips">
      <li v-for="(tip, i) in suggestions" :key="i">{{ tip }}</li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BudgetCheckResult, Itinerary } from '@/types/plan'

const props = defineProps<{
  itinerary: Itinerary
  budget?: BudgetCheckResult | null
}>()

const fmt = (v: number) => v.toLocaleString('zh-CN', { maximumFractionDigits: 0 })

// 有真实预算检查结果时使用；否则基于行程费用估算（标注估算）
const budget = computed(() => props.budget?.budget ?? props.itinerary.budget ?? 0)
const activityCost = computed(() => props.budget?.knownActivityCosts ?? props.itinerary.estimatedActivityCost ?? 0)
const additionalCosts = computed(() => props.budget?.additionalCosts ?? 0)
const remaining = computed(() =>
  props.budget ? props.budget.remainingBudget : budget.value - activityCost.value,
)
const overBudget = computed(() => (props.budget ? props.budget.overBudget : remaining.value < 0))
const used = computed(() => Math.max(0, budget.value - remaining.value))
const barPercent = computed(() => {
  if (budget.value <= 0) return 0
  return Math.min(100, Math.max(0, (used.value / budget.value) * 100))
})
const suggestions = computed(() => props.budget?.suggestions ?? [])
const statusClass = computed(() => (overBudget.value ? 'is-over' : 'is-ok'))
</script>

<style scoped>
.budget-summary {
  border-radius: 14px;
  padding: 14px 16px;
}

.budget-summary.is-ok {
  background: var(--brand-softer);
  border: 1px solid rgba(76, 154, 110, 0.3);
}

.budget-summary.is-over {
  background: rgba(201, 106, 90, 0.07);
  border: 1px solid rgba(201, 106, 90, 0.3);
}

.bs-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.bs-item {
  flex: 1 1 120px;
}

.bs-label {
  display: block;
  font-size: 11.5px;
  color: var(--text-secondary);
  margin-bottom: 2px;
}

.bs-item b {
  font-size: 15px;
  color: var(--text);
}

.bs-item.is-over .bs-balance {
  color: var(--danger);
}

.bs-bar {
  height: 8px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 4px;
  margin-top: 14px;
  overflow: hidden;
}

.bs-bar-fill {
  height: 100%;
  border-radius: 4px;
  background: var(--success);
  transition: width 0.3s;
}

.budget-summary.is-over .bs-bar-fill {
  background: var(--danger);
}

.bs-bar-caption {
  font-size: 11px;
  color: var(--text-faint);
  margin-top: 4px;
}

.bs-tips {
  margin: 10px 0 0;
  padding-left: 18px;
  font-size: 12.5px;
  color: var(--text-secondary);
  line-height: 1.7;
}
</style>
