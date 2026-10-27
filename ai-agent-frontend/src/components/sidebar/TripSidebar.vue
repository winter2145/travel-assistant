<template>
  <aside class="trip-sidebar">
    <div class="trip-header">
      <span class="trip-title">当前旅行</span>
      <span v-if="planStore.source === 'demo'" class="demo-tag">演示数据</span>
      <span v-else-if="planStore.itinerary" class="live-tag">来自对话</span>
    </div>

    <!-- 空态 -->
    <div v-if="!planStore.itinerary" class="trip-empty">
      <el-icon><Compass /></el-icon>
      <p>在对话中首次描述旅行需求，AI 生成行程后这里会显示其概览</p>
    </div>

    <template v-else>
      <!-- 概览卡 -->
      <div class="overview-card">
        <div class="ov-row">
          <span class="ov-label">目的地</span>
          <span class="ov-value">{{ planStore.itinerary.city || '—' }}</span>
        </div>
        <div class="ov-row">
          <span class="ov-label">出发日期</span>
          <span class="ov-value">{{ planStore.itinerary.startDate || '—' }}</span>
        </div>
        <div class="ov-row">
          <span class="ov-label">天数</span>
          <span class="ov-value">{{ planStore.totalDays }} 天</span>
        </div>
        <div class="ov-row">
          <span class="ov-label">活动</span>
          <span class="ov-value">{{ planStore.totalActivities }} 项</span>
        </div>
        <div class="ov-row">
          <span class="ov-label">节奏</span>
          <span class="ov-value">{{ paceText }}</span>
        </div>
        <div class="ov-row">
          <span class="ov-label">总预算</span>
          <span class="ov-value">¥{{ planStore.itinerary.budget.toLocaleString() }}</span>
        </div>
        <div class="ov-row">
          <span class="ov-label">活动费用</span>
          <span class="ov-value">¥{{ planStore.activityCost.toLocaleString() }}</span>
        </div>
      </div>

      <!-- 每日行程摘要 -->
      <div class="section-title">每日行程</div>
      <div v-for="day in planStore.itinerary.days" :key="day.day" class="day-summary">
        <div class="day-head">
          <span class="day-badge">D{{ day.day }}</span>
          <span class="day-date">{{ day.date }}</span>
          <span class="day-count">{{ day.activities.length }} 项</span>
        </div>
        <div class="day-places">
          <span v-for="(act, i) in day.activities" :key="i" class="place-chip">{{ act.name }}</span>
        </div>
      </div>

      <!-- 预算摘要 -->
      <template v-if="planStore.budget">
        <div class="section-title">预算检查</div>
        <div class="budget-card" :class="planStore.budget.overBudget ? 'is-over' : 'is-ok'">
          <div class="budget-main">
            <span>预计总费用</span>
            <b>¥{{ planStore.budget.estimatedTotal.toLocaleString() }}</b>
          </div>
          <div class="budget-sub">
            剩余 {{ planStore.budget.remainingBudget >= 0 ? '¥' + planStore.budget.remainingBudget.toLocaleString() : '已超支 ¥' + Math.abs(planStore.budget.remainingBudget).toLocaleString() }}
          </div>
        </div>
      </template>
      <div v-else class="section-title" style="margin-top: 14px">
        <span class="pending-tag">待接入</span> 预算检查结果随对话生成
      </div>
    </template>

    <!-- 操作区 -->
    <div class="trip-actions">
      <el-button
        type="primary"
        class="action-btn"
        :disabled="!planStore.itinerary"
        @click="goDetail"
      >
        <el-icon><Calendar /></el-icon>查看完整行程
      </el-button>
      <el-button
        class="action-btn"
        :disabled="!planStore.itinerary"
        @click="goGuide"
      >
        <el-icon><Document /></el-icon>生成旅游攻略 PDF
      </el-button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { usePlanStore } from '@/stores/plan'

const planStore = usePlanStore()
const router = useRouter()

const paceText = computed(() => {
  const map: Record<string, string> = { relaxed: '宽松', normal: '常规', compact: '紧凑' }
  return map[planStore.itinerary?.pace ?? ''] ?? planStore.itinerary?.pace ?? '—'
})

function goDetail() {
  void router.push({ name: 'travel-itinerary', params: { id: planStore.planId ?? 'current' } })
}

function goGuide() {
  void router.push({ name: 'travel-export', params: { id: planStore.planId ?? 'current' } })
}
</script>

<style scoped>
.trip-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fffefa;
  overflow-y: auto;
}

.trip-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 16px 14px;
  border-bottom: 1px solid #f0eee8;
  position: sticky;
  top: 0;
  background: var(--bg-card);
  z-index: 2;
}

.trip-title {
  color: #314235;
  font-family: Georgia, 'Songti SC', serif;
  font-size: 17px;
  font-weight: 500;
}

.live-tag {
  display: inline-flex;
  font-size: 11px;
  color: var(--success);
  background: rgba(76, 154, 110, 0.12);
  border: 1px solid rgba(76, 154, 110, 0.35);
  border-radius: 999px;
  padding: 1px 8px;
  line-height: 18px;
}

.trip-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin: 18px 14px;
  padding: 28px 18px;
  border: 1px dashed #dfe4da;
  border-radius: 14px;
  background: #fbfcf9;
  text-align: center;
  color: var(--text-faint);
  font-size: 13px;
}

.trip-empty .el-icon {
  font-size: 36px;
}

.overview-card {
  margin: 14px 14px 4px;
  padding: 13px 14px;
  border: 1px solid #eeece4;
  border-radius: 13px;
  background: #fff;
}

.ov-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
}

.ov-label {
  color: var(--text-secondary);
}

.ov-value {
  color: var(--text);
  font-weight: 500;
}

.section-title {
  font-size: 12px;
  color: var(--text-faint);
  padding: 14px 16px 7px;
}

.day-summary {
  padding: 0 16px 10px;
}

.day-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.day-badge {
  background: var(--brand);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 5px;
  padding: 1px 7px;
}

.day-date {
  font-size: 12px;
  color: var(--text-secondary);
}

.day-count {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-faint);
}

.day-places {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.place-chip {
  font-size: 11.5px;
  background: var(--bg-muted);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 2px 8px;
  color: var(--text);
}

.budget-card {
  margin: 0 14px 14px;
  border-radius: 10px;
  padding: 10px 12px;
}

.budget-card.is-ok {
  background: var(--brand-softer);
  border: 1px solid rgba(76, 154, 110, 0.3);
}

.budget-card.is-over {
  background: rgba(201, 106, 90, 0.08);
  border: 1px solid rgba(201, 106, 90, 0.3);
}

.budget-main {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.budget-main b {
  color: var(--brand);
}

.budget-card.is-over .budget-main b {
  color: var(--danger);
}

.budget-sub {
  font-size: 11.5px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.trip-actions {
  padding: 12px 14px 16px;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  border-top: 1px solid #f0eee8;
  margin-top: auto;
}

.action-btn {
  width: 100%;
  min-width: 0;
  height: 40px;
  margin: 0;
  border-radius: 10px;
}
</style>
