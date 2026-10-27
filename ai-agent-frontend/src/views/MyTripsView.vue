<template>
  <div class="my-trips">
    <AppHeader />

    <div class="page-container">
      <div class="trips-header">
        <div>
          <h1 class="page-title">我的旅行</h1>
          <p class="page-subtitle">
            管理你生成的旅行计划
          </p>
        </div>
        <el-button type="primary" @click="router.push('/')">
          <el-icon><Plus /></el-icon>新建旅行
        </el-button>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索目的地或计划名称"
          clearable
          style="max-width: 320px"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <span class="trips-count">{{ rows.length }} 份旅行计划</span>
      </div>

      <!-- 列表 -->
      <div v-if="filteredTrips.length > 0" class="trips-grid">
        <div v-for="trip in filteredTrips" :key="trip.id" class="trip-card">
          <div class="trip-card-head">
            <span class="trip-city">{{ trip.city }}</span>
            <span class="live-tag">已保存</span>
          </div>
          <div class="trip-title">{{ trip.title }}</div>
          <div class="trip-meta">
            <span>{{ trip.startDate }} 出发</span>
            <span>{{ trip.days }} 天 · {{ trip.activities }} 项活动</span>
            <span>预算 ¥{{ trip.budget.toLocaleString() }}</span>
          </div>
          <div class="trip-actions">
          <el-button text size="small" type="primary" @click="continueEdit(trip)">
              <el-icon><Edit /></el-icon>继续编辑
            </el-button>
            <el-button text size="small" @click="duplicate(trip)">
              <el-icon><CopyDocument /></el-icon>复制
            </el-button>
            <el-button text size="small" type="danger" @click="remove(trip)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="trips-empty">
        <EmptyState icon="Suitcase" :title="keyword ? '没有匹配的旅行' : '还没有旅行计划'" description="向 AI 描述你的旅行需求，生成第一份行程计划。">
          <el-button type="primary" @click="router.push('/')">去工作台创建</el-button>
        </EmptyState>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/layouts/AppHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { listPlans, deletePlan, duplicatePlan } from '@/api/plan'
import type { TravelPlan } from '@/types/plan'

const router = useRouter()
const keyword = ref('')
const rows = ref<TravelPlan[]>([])
const loading = ref(false)

async function refresh() {
  loading.value = true
  try {
    rows.value = (await listPlans({ page: 1, pageSize: 100, keyword: keyword.value || undefined })).list
  } catch {
    ElMessage.warning('暂时无法读取旅行计划，请稍后重试。')
  } finally {
    loading.value = false
  }
}

onMounted(() => void refresh())

const filteredTrips = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return rows.value.filter((t) => !kw || t.city.toLowerCase().includes(kw) || t.title.toLowerCase().includes(kw))
    .map((plan) => ({
      id: plan.id,
      title: plan.title,
      city: plan.city,
      startDate: plan.startDate,
      days: plan.days,
      activities: plan.itinerary?.days.reduce((sum, day) => sum + day.activities.length, 0) ?? 0,
      budget: plan.budget,
    }))
})

function continueEdit(trip: { id: string }) {
  void router.push({ name: 'plan-detail', params: { id: trip.id } })
}

async function duplicate(trip: { id: string }) {
  try {
    const copy = await duplicatePlan(trip.id)
    rows.value = [copy, ...rows.value]
    ElMessage.success('已复制旅行计划')
  } catch {
    ElMessage.warning('复制旅行计划失败，请稍后重试。')
  }
}

function remove(trip: { id: string; title: string }) {
  void ElMessageBox.confirm(`确定删除「${trip.title}」吗？`, '删除旅行', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      void deletePlan(trip.id).then(() => {
        rows.value = rows.value.filter((row) => row.id !== trip.id)
        ElMessage.success('已删除旅行计划')
      }).catch(() => ElMessage.warning('删除旅行计划失败，请稍后重试。'))
    })
    .catch(() => {})
}
</script>

<style scoped>
.trips-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.trips-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.trip-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px 16px;
  transition: box-shadow 0.15s;
}

.trip-card:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}

.trip-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.trip-city {
  font-size: 15px;
  font-weight: 700;
  color: var(--brand);
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

.trip-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.trip-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 12.5px;
  color: var(--text-secondary);
  margin-bottom: 10px;
}

.trip-actions {
  display: flex;
  gap: 4px;
  border-top: 1px solid var(--border);
  padding-top: 8px;
}

.trips-empty {
  background: var(--bg-card);
  border: 1px dashed var(--border-strong);
  border-radius: 14px;
}
</style>
