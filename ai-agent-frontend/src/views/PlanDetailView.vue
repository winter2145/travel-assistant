<template>
  <div class="plan-detail">
    <AppHeader />

    <div class="page-container plan-body">
      <!-- 头部 -->
      <div class="plan-header">
        <div>
          <h1 class="page-title">{{ title }}</h1>
          <p class="page-subtitle">
            {{ metaText }}
            <span v-if="planStore.source === 'demo'" class="demo-tag" style="margin-left: 8px">演示数据</span>
            <span v-else class="live-tag">来自真实对话</span>
          </p>
        </div>
        <div class="plan-header-actions">
          <el-button @click="goReplan">
            <el-icon><MagicStick /></el-icon>请求 AI 重新规划
          </el-button>
          <el-button type="primary" @click="goGuide">
            <el-icon><Document /></el-icon>生成攻略
          </el-button>
        </div>
      </div>

      <!-- 编辑工具条 -->
      <div class="edit-bar">
        <span class="edit-hint">编辑后保存即可同步到旅行记录</span>
        <el-button text size="small" :disabled="!planStore.itinerary" @click="saveLocal">
          <el-icon><Check /></el-icon>保存行程
        </el-button>
        <el-button text size="small" :disabled="!planStore.itinerary" @click="copyPlan">
          <el-icon><CopyDocument /></el-icon>复制计划
        </el-button>
      </div>

      <!-- 无数据空态 -->
      <div v-if="!planStore.itinerary" class="plan-empty">
        <EmptyState icon="Compass" title="还没有可展示的行程" description="回到工作台，向 AI 描述旅行需求生成行程后，再回来查看详情。">
          <el-button type="primary" @click="router.push('/')">去工作台生成行程</el-button>
        </EmptyState>
      </div>

      <template v-else>
        <!-- 预算摘要 -->
        <div class="section-block">
          <h2 class="section-title">预算概览</h2>
          <BudgetSummary :itinerary="planStore.itinerary" :budget="planStore.budget ?? null" />
          <p v-if="!planStore.budget" class="section-note">预算余额为基于行程费用的估算；对话中执行预算检查后会展示完整明细。</p>
        </div>

        <!-- 每日行程 -->
        <div class="section-block">
          <h2 class="section-title">每日行程</h2>
          <DayTimeline
            v-for="day in planStore.itinerary.days"
            :key="day.day"
            :day="day"
            @add="openAdd(day.day)"
            @edit="openEdit(day.day, $event)"
            @remove="removeActivity(day.day, $event)"
            @move="moveActivity(day.day, $event)"
          />
        </div>

        <div class="section-block">
          <h2 class="section-title">地图路线与独立路线查询</h2>
          <RoutePlanner :city="planStore.itinerary.city" :default-origin="routeDefaults.origin" :default-destination="routeDefaults.destination" />
        </div>
      </template>
    </div>

    <!-- 添加/编辑活动弹窗 -->
    <ActivityEditDialog
      v-model="dialogVisible"
      :mode="dialogMode"
      :initial="editingActivity"
      @confirm="onActivityConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/layouts/AppHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import DayTimeline from '@/components/plan/DayTimeline.vue'
import BudgetSummary from '@/components/plan/BudgetSummary.vue'
import ActivityEditDialog from '@/components/plan/ActivityEditDialog.vue'
import RoutePlanner from '@/components/plan/RoutePlanner.vue'
import { usePlanStore } from '@/stores/plan'
import type { Itinerary, ItineraryActivity } from '@/types/plan'
import { getPlan, savePlan, updatePlan, duplicatePlan } from '@/api/plan'
import { useConversationStore } from '@/stores/conversation'

const route = useRoute()
const router = useRouter()
const planStore = usePlanStore()
const conversationStore = useConversationStore()

// 若从工作台进入（id=current），使用 store；否则标记为"后端详情接口待接入"并回退 demo
const isCurrent = computed(() => route.params.id === 'current')

onMounted(async () => {
  if (isCurrent.value || !route.params.id) return
  try {
    const plan = await getPlan(String(route.params.id))
    if (plan.itinerary) planStore.loadPlanRecord(plan)
  } catch {
    ElMessage.warning('暂时无法读取这份行程，请从我的旅行重新打开。')
  }
})

const title = computed(() => (planStore.itinerary ? `${planStore.itinerary.city}旅行计划` : '行程详情'))
const metaText = computed(() => {
  if (!planStore.itinerary) return ''
  const it = planStore.itinerary
  return `${it.city} · ${it.startDate} 出发 · ${it.days.length} 天 · ${it.days.reduce((s, d) => s + d.activities.length, 0)} 项活动 · 预算 ¥${it.budget.toLocaleString()}`
})
const routeDefaults = computed(() => {
  const activities = planStore.itinerary?.days.flatMap((day) => day.activities) ?? []
  return {
    origin: activities[0]?.location || activities[0]?.name || '',
    destination: activities[1]?.location || activities[1]?.name || '',
  }
})

/* ---------- 活动编辑（本地预览态） ---------- */
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const editingDay = ref(0)
const editingIndex = ref(-1)
const editingActivity = ref<ItineraryActivity | null>(null)

function openAdd(day: number) {
  dialogMode.value = 'add'
  editingDay.value = day
  editingIndex.value = -1
  editingActivity.value = null
  dialogVisible.value = true
}

function openEdit(day: number, index: number) {
  const it = planStore.itinerary
  if (!it) return
  const dayObj = it.days.find((d) => d.day === day)
  const activity = dayObj?.activities[index]
  if (!activity) return
  dialogMode.value = 'edit'
  editingDay.value = day
  editingIndex.value = index
  editingActivity.value = { ...activity }
  dialogVisible.value = true
}

function onActivityConfirm(form: {
  name: string
  location: string
  startTime: string
  endTime: string
  estimatedCost: number
  travelMinutes: number
  notes: string
}) {
  const it = planStore.itinerary
  if (!it) return
  const dayObj = it.days.find((d) => d.day === editingDay.value)
  if (!dayObj) return

  if (dialogMode.value === 'add') {
    dayObj.activities.push({
      name: form.name,
      location: form.location || undefined,
      startTime: form.startTime,
      endTime: form.endTime,
      estimatedCost: form.estimatedCost,
      travelMinutesFromPrevious: form.travelMinutes,
      notes: form.notes || undefined,
    })
  } else if (editingIndex.value >= 0) {
    dayObj.activities[editingIndex.value] = {
      ...dayObj.activities[editingIndex.value],
      name: form.name,
      location: form.location || undefined,
      startTime: form.startTime,
      endTime: form.endTime,
      estimatedCost: form.estimatedCost,
      travelMinutesFromPrevious: form.travelMinutes,
      notes: form.notes || undefined,
    }
  }
  planStore.setItinerary(structuredClone(it))
  ElMessage.success(dialogMode.value === 'add' ? '已添加活动' : '已更新活动')
}

function removeActivity(day: number, index: number) {
  const it = planStore.itinerary
  if (!it) return
  const dayObj = it.days.find((d) => d.day === day)
  if (!dayObj) return
  dayObj.activities.splice(index, 1)
  planStore.setItinerary(structuredClone(it))
  ElMessage.success('已删除活动')
}

function moveActivity(day: number, payload: { index: number; dir: number }) {
  const it = planStore.itinerary
  if (!it) return
  const dayObj = it.days.find((d) => d.day === day)
  if (!dayObj) return
  const { index, dir } = payload
  const target = index + dir
  if (target < 0 || target >= dayObj.activities.length) return
  const [item] = dayObj.activities.splice(index, 1)
  dayObj.activities.splice(target, 0, item)
  planStore.setItinerary(structuredClone(it))
}

async function saveLocal() {
  const itinerary = planStore.itinerary
  if (!itinerary) return
  try {
    const payload = { title: `${itinerary.city}旅行计划`, itinerary, budgetSummary: planStore.budget }
    const saved = planStore.planId
      ? await updatePlan(planStore.planId, { ...payload, version: planStore.planVersion })
      : await savePlan({ ...payload, conversationId: conversationStore.activeId ?? '' })
    planStore.setSavedPlan(saved.id, saved.version)
    ElMessage.success('行程已保存')
  } catch (error) {
    ElMessage.warning((error as Error).message || '保存行程失败，请稍后重试。')
  }
}

async function copyPlan() {
  if (!planStore.itinerary) return
  if (planStore.planId) {
    try {
      const saved = await duplicatePlan(planStore.planId)
      planStore.loadPlanRecord(saved)
      ElMessage.success('已复制并保存为新旅行计划')
      return
    } catch {
      ElMessage.warning('复制行程失败，请稍后重试。')
      return
    }
  }
  const it: Itinerary = structuredClone(planStore.itinerary)
  it.startDate = new Date(Date.now() + 86400000).toISOString().slice(0, 10)
  planStore.setItinerary(it)
  ElMessage.success('已复制计划（新出发日期）')
}

function goReplan() {
  void router.push({
    name: 'workbench',
    query: { prompt: `请重新规划 ${planStore.itinerary?.city ?? ''} 的行程` },
  })
}

function goGuide() {
  void router.push({ name: 'travel-export', params: { id: route.params.id } })
}
</script>

<style scoped>
.plan-body {
  padding-bottom: 48px;
}

.plan-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.plan-header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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
  margin-left: 8px;
}

.edit-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 8px 14px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.edit-hint {
  font-size: 12px;
  color: var(--text-faint);
  flex: 1;
}

.section-block {
  margin-bottom: 22px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px;
  display: flex;
  align-items: center;
}

.section-note {
  font-size: 12px;
  color: var(--text-faint);
  margin: 8px 0 0;
}

.plan-empty {
  background: var(--bg-card);
  border: 1px dashed var(--border-strong);
  border-radius: 14px;
  padding: 24px;
}

.map-placeholder {
  background: var(--bg-card);
  border: 1px dashed var(--border-strong);
  border-radius: 14px;
  padding: 40px 20px;
  text-align: center;
  color: var(--text-secondary);
}

.map-placeholder .el-icon {
  font-size: 36px;
  color: var(--border-strong);
  margin-bottom: 8px;
}

.map-placeholder p {
  margin: 4px 0;
  font-size: 13px;
}

.map-note {
  font-size: 12px;
  color: var(--text-faint) !important;
}
</style>
