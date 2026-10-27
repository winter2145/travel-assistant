<template>
  <div class="guide-preview">
    <AppHeader />

    <div class="page-container guide-body">
      <div class="guide-header">
        <div>
          <span class="guide-kicker">YOUR TRIP, BEAUTIFULLY GATHERED</span>
          <h1 class="page-title">攻略预览与导出</h1>
          <p class="page-subtitle">
            {{ guideTitle }}
            <span v-if="planStore.source === 'demo'" class="demo-tag" style="margin-left: 8px">演示数据</span>
            <span v-else class="live-tag">来自真实对话</span>
          </p>
        </div>
      </div>

      <!-- 无数据空态 -->
      <div v-if="!planStore.itinerary" class="guide-empty">
        <EmptyState icon="Document" title="还没有可生成攻略的行程" description="回到工作台生成行程后，再来这里导出 PDF 攻略。">
          <el-button type="primary" @click="router.push('/')">去工作台生成行程</el-button>
        </EmptyState>
      </div>

      <template v-else>
        <div class="guide-layout">
          <aside class="guide-settings">
            <div class="settings-heading"><span class="settings-icon"><el-icon><Operation /></el-icon></span><div><strong>整理你的攻略</strong><small>选择想带走的内容</small></div></div>
        <!-- 配置面板 -->
        <div class="config-panel">
          <div class="config-group">
            <span class="config-label">攻略风格</span>
            <el-radio-group v-model="style" size="default">
              <el-radio-button value="simple">简洁版</el-radio-button>
              <el-radio-button value="magazine">图文杂志版</el-radio-button>
            </el-radio-group>
          </div>

          <div class="config-group">
            <span class="config-label">内容模块</span>
            <el-checkbox-group v-model="sections">
              <el-checkbox value="cover">封面</el-checkbox>
              <el-checkbox value="overview">行程概览</el-checkbox>
              <el-checkbox value="budget">预算表</el-checkbox>
              <el-checkbox value="itinerary">每日行程</el-checkbox>
              <el-checkbox value="photos">景点图片</el-checkbox>
            </el-checkbox-group>
          </div>

          <div class="config-actions">
            <el-button type="primary" :loading="exporting" @click="onGeneratePdf">
              <el-icon><Printer /></el-icon>生成并下载 PDF
            </el-button>
            <span class="export-note">预览会随选项即时更新</span>
          </div>
        </div>

          </aside>
          <main class="guide-preview-stage">
            <div class="preview-toolbar"><div><strong>实时预览</strong><span>A4 · {{ style === 'magazine' ? '图文杂志版' : '简洁版' }}</span></div><span class="preview-page">{{ sections.length }} 个内容模块</span></div>

        <!-- 预览区 -->
        <div class="preview-frame" :class="`is-${style}`">
          <!-- 封面 -->
          <div v-if="sections.includes('cover')" class="preview-cover">
            <div class="cover-city">{{ planStore.itinerary.city }}</div>
            <div class="cover-sub">{{ planStore.itinerary.days.length }} 天 {{ planStore.totalActivities }} 项活动</div>
            <div class="cover-date">{{ planStore.itinerary.startDate }} 出发</div>
          </div>

          <!-- 概览 -->
          <div v-if="sections.includes('overview')" class="preview-overview">
            <div class="ov-card" v-for="(item, i) in overviewItems" :key="i">
              <div class="ov-card-label">{{ item.label }}</div>
              <div class="ov-card-value">{{ item.value }}</div>
            </div>
          </div>

          <!-- 预算表 -->
          <div v-if="sections.includes('budget')" class="preview-section">
            <h3 class="preview-h3">预算表（单位：元）</h3>
            <BudgetSummary :itinerary="planStore.itinerary" :budget="planStore.budget ?? null" />
            <p v-if="!planStore.budget" class="preview-note">余额为基于活动费用的估算。</p>
          </div>

          <!-- 每日行程 -->
          <div v-if="sections.includes('itinerary')" class="preview-section">
            <h3 class="preview-h3">每日行程</h3>
            <div v-for="day in planStore.itinerary.days" :key="day.day" class="preview-day">
              <div class="preview-day-head">
                <span class="pd-badge">Day {{ day.day }}</span>
                <span>{{ day.date }}</span>
              </div>
              <table class="preview-table">
                <thead>
                  <tr><th>时间</th><th>活动</th><th>地点</th><th>费用</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(act, i) in day.activities" :key="i">
                    <td>{{ act.startTime }}–{{ act.endTime }}</td>
                    <td>{{ act.name }}</td>
                    <td>{{ act.location || '—' }}</td>
                    <td>{{ act.estimatedCost > 0 ? '¥' + act.estimatedCost : '免费' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 景点图片（图文杂志版） -->
          <div v-if="sections.includes('photos')" class="preview-section">
            <h3 class="preview-h3">景点一览</h3>
            <div class="photo-grid">
              <div v-for="(act, i) in allActivities" :key="i" class="photo-card">
                <a v-if="act.photoUrl" class="photo-placeholder" :href="act.photoPageUrl" target="_blank" rel="noopener noreferrer">
                  <img :src="act.photoUrl" :alt="act.name" loading="lazy" />
                </a>
                <div v-else class="photo-placeholder" aria-label="暂无已保存图片"><el-icon><Picture /></el-icon></div>
                <div class="photo-name">{{ act.name }}</div>
                <small v-if="act.photoPhotographer">图片：{{ act.photoPhotographer }} · {{ act.photoSource || 'Pexels' }}</small>
                <small v-else-if="act.photoSearchError" class="photo-error">图片未获取：{{ act.photoSearchError }}</small>
              </div>
            </div>
            <p class="preview-note">图片在生成行程时通过 Pexels MCP 查询并随行程保存；没有图片的景点会保留空位。</p>
          </div>
        </div>
          </main>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/layouts/AppHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import BudgetSummary from '@/components/plan/BudgetSummary.vue'
import { usePlanStore } from '@/stores/plan'
import { getPlan } from '@/api/plan'
import { savePlan, updatePlan } from '@/api/plan'
import { createPdfExport } from '@/api/pdf'
import { useConversationStore } from '@/stores/conversation'

const route = useRoute()
const router = useRouter()
const planStore = usePlanStore()
const conversationStore = useConversationStore()

const isCurrent = computed(() => route.params.id === 'current')
onMounted(async () => {
  if (isCurrent.value || planStore.itinerary || !route.params.id) return
  try {
    const plan = await getPlan(String(route.params.id))
    if (plan.itinerary) planStore.loadPlanRecord(plan)
  } catch {
    ElMessage.warning('暂时无法读取这份行程，请从我的旅行重新打开。')
  }
})

const style = ref<'simple' | 'magazine'>('simple')
const sections = ref<string[]>(['cover', 'overview', 'budget', 'itinerary'])
const exporting = ref(false)

const guideTitle = computed(() => {
  const it = planStore.itinerary
  return it ? `${it.city} · ${it.days.length} 天旅游攻略` : '攻略预览'
})

const overviewItems = computed(() => {
  const it = planStore.itinerary
  if (!it) return []
  return [
    { label: '目的地', value: it.city },
    { label: '出发日期', value: it.startDate },
    { label: '行程天数', value: `${it.days.length} 天` },
    { label: '总预算', value: `¥${it.budget.toLocaleString()}` },
  ]
})

const allActivities = computed(() => {
  const it = planStore.itinerary
  if (!it) return []
  return it.days.flatMap((d) => d.activities)
})

async function onGeneratePdf() {
  const itinerary = planStore.itinerary
  if (!itinerary || exporting.value) return
  exporting.value = true
  try {
    const payload = { title: `${itinerary.city}旅行计划`, itinerary, budgetSummary: planStore.budget }
    const plan = planStore.planId
      ? await updatePlan(planStore.planId, { ...payload, version: planStore.planVersion })
      : await savePlan({ ...payload, conversationId: conversationStore.activeId ?? '' })
    planStore.setSavedPlan(plan.id, plan.version)
    const blob = await createPdfExport(plan.id, {
      planId: plan.id,
      style: style.value,
      includeCover: sections.value.includes('cover'),
      includeOverview: sections.value.includes('overview'),
      includePhotos: sections.value.includes('photos'),
      includeBudget: sections.value.includes('budget'),
      includeItinerary: sections.value.includes('itinerary') || sections.value.includes('photos'),
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${itinerary.city}_旅行攻略.pdf`
    document.body.append(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    ElMessage.success('PDF 攻略已生成并下载')
  } catch (error) {
    ElMessage.error((error as Error).message || 'PDF 生成失败，请检查后端配置后重试。')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.guide-body {
  padding-bottom: 48px;
  max-width: 1450px;
}

.guide-header {
  margin: 10px 0 20px;
}
.guide-kicker { color: #9aa399; font-size: 9px; font-weight: 700; letter-spacing: 1.5px; }
.guide-header .page-title { margin-top: 4px; font-family: Georgia, 'Songti SC', serif; font-size: 25px; font-weight: 500; }
.guide-layout { display: grid; grid-template-columns: 292px minmax(0, 1fr); gap: 22px; align-items: start; }
.guide-settings { position: sticky; top: 76px; padding: 19px; border: 1px solid #e8e6dd; border-radius: 16px; background: #fffefa; box-shadow: 0 8px 24px rgba(38,55,42,.04); }
.settings-heading { display: flex; align-items: center; gap: 11px; padding-bottom: 16px; margin-bottom: 3px; border-bottom: 1px solid #efeee8; }
.settings-icon { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 11px; color: #547458; background: #edf2e9; font-size: 17px; }
.settings-heading div { display: grid; gap: 2px; }
.settings-heading strong { color: #344439; font-size: 13px; }
.settings-heading small { color: #9aa199; font-size: 10px; }
.guide-preview-stage { min-width: 0; }
.preview-toolbar { display: flex; align-items: center; justify-content: space-between; max-width: 850px; margin: 0 auto 11px; color: #526157; }
.preview-toolbar > div { display: flex; align-items: center; gap: 10px; }
.preview-toolbar strong { font-size: 12px; }
.preview-toolbar span { color: #9ba39c; font-size: 10px; }
.preview-toolbar .preview-page { padding: 4px 9px; border: 1px solid #e5e8e0; border-radius: 999px; background: #fbfcf9; }

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

.guide-empty {
  background: var(--bg-card);
  border: 1px dashed var(--border-strong);
  border-radius: 14px;
  padding: 24px;
}

.config-panel {
  background: transparent;
  border: 0;
  border-radius: 0;
  padding: 14px 0 4px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 16px;
}

.config-group {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 8px;
  flex-wrap: wrap;
}

.config-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  flex: 0 0 auto;
  min-width: 64px;
}
.config-group :deep(.el-checkbox-group) { display: grid; grid-template-columns: 1fr 1fr; gap: 4px 0; }
.config-group :deep(.el-radio-group) { display: flex; width: 100%; }
.config-group :deep(.el-radio-button) { flex: 1; }
.config-group :deep(.el-radio-button__inner) { width: 100%; padding: 8px 7px; font-size: 11px; }
.config-group :deep(.el-checkbox) { margin-right: 0; height: 28px; }
.config-group :deep(.el-checkbox__label) { color: #667269; font-size: 11px; }

.config-actions {
  display: flex;
  align-items: stretch;
  flex-direction: column;
  gap: 8px;
  border-top: 1px solid var(--border);
  padding-top: 12px;
  flex-wrap: wrap;
}
.config-actions :deep(.el-button) { width: 100%; min-height: 39px; border-radius: 10px; font-weight: 600; }
.export-note { color: #99a198; font-size: 10px; text-align: center; }

.pdf-status {
  display: flex;
  align-items: center;
  gap: 8px;
  border-radius: 10px;
  padding: 10px 14px;
  margin-bottom: 16px;
  font-size: 13px;
}

.pdf-status.is-generating {
  background: var(--brand-softer);
  color: var(--brand);
}

.pdf-status.is-success {
  background: rgba(76, 154, 110, 0.1);
  color: var(--success);
}

.pdf-status.is-failed {
  background: rgba(201, 106, 90, 0.08);
  color: var(--danger);
}

.preview-frame {
  width: 100%;
  max-width: 850px;
  min-height: 900px;
  margin: 0 auto;
  background: #fff;
  border: 1px solid #e5e3dd;
  border-radius: 5px;
  padding: 42px clamp(22px, 5vw, 62px);
  box-shadow: 0 16px 42px rgba(38, 55, 42, .08);
}

/* 封面 */
.preview-cover {
  background: linear-gradient(135deg, #2f6b4f 0%, #3d8a63 60%, #5a9a76 100%);
  border-radius: 16px;
  color: #fff;
  text-align: center;
  padding: 48px 24px;
  margin-bottom: 18px;
}

.cover-city {
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 4px;
}

.cover-sub {
  font-size: 14px;
  opacity: 0.9;
  margin-top: 6px;
}

.cover-date {
  font-size: 12px;
  opacity: 0.75;
  margin-top: 12px;
}

/* 概览 */
.preview-overview {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
}

.ov-card {
  flex: 1 1 140px;
  background: var(--brand-softer);
  border-radius: 12px;
  padding: 12px 14px;
}

.ov-card-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.ov-card-value {
  font-size: 16px;
  font-weight: 600;
  margin-top: 2px;
}

/* 章节 */
.preview-section {
  margin-bottom: 18px;
}

.preview-h3 {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 10px;
  padding-left: 10px;
  border-left: 3px solid var(--brand);
}

.preview-day {
  margin-bottom: 14px;
}

.preview-day-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}

.pd-badge {
  background: var(--brand);
  color: #fff;
  border-radius: 5px;
  padding: 1px 8px;
  font-size: 12px;
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.preview-table th,
.preview-table td {
  border: 1px solid var(--border);
  padding: 7px 10px;
  text-align: left;
}

.preview-table th {
  background: var(--bg-muted);
  font-weight: 600;
}

.preview-note {
  font-size: 12px;
  color: var(--text-faint);
  margin-top: 8px;
}

/* 图文杂志版照片区 */
.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.photo-card {
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
}

.photo-placeholder {
  height: 90px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: var(--bg-muted);
  color: var(--border-strong);
  font-size: 26px;
}
.photo-placeholder img { width: 100%; height: 100%; object-fit: cover; }
.photo-card small { display: block; padding: 0 8px 8px; color: var(--text-faint); font-size: 10px; text-align: center; }
.photo-card .photo-error { color: var(--danger, #b45c4e); }

.photo-name {
  font-size: 12.5px;
  padding: 8px 10px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 杂志版增强：行程卡片化 */
.preview-frame.is-magazine .preview-day {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px 14px;
}

@media (max-width: 900px) {
  .guide-layout { grid-template-columns: 1fr; }
  .guide-settings { position: static; }
  .config-panel { display: grid; grid-template-columns: 1fr 1.2fr; gap: 16px; }
  .config-actions { grid-column: 1 / -1; flex-direction: row; align-items: center; }
  .config-actions :deep(.el-button) { width: auto; }
}
@media (max-width: 600px) {
  .page-container.guide-body { padding: 14px 14px 30px; }
  .config-panel { display: flex; }
  .config-actions { flex-direction: column; align-items: stretch; }
  .config-actions :deep(.el-button) { width: 100%; }
  .preview-frame { min-height: 0; padding: 22px 16px; border-radius: 8px; }
  .cover-city { font-size: 28px; }
  .preview-table { font-size: 10px; }
  .preview-table th, .preview-table td { padding: 5px; }
}

@media print {
  @page { size: A4; margin: 14mm; }
  :global(body), :global(html), :global(#app) { height: auto !important; background: #fff !important; }
  :global(.app-header), .guide-header, .guide-settings, .preview-toolbar, .guide-empty { display: none !important; }
  .page-container.guide-body { max-width: none; padding: 0; margin: 0; }
  .guide-layout { display: block; }
  .guide-preview-stage { width: 100%; }
  .preview-frame { max-width: none; min-height: 0; border: 0; border-radius: 0; padding: 0; box-shadow: none; }
  .preview-cover { break-inside: avoid; }
  .preview-day { break-inside: avoid; }
  .preview-table { font-size: 10pt; }
  .ov-card { break-inside: avoid; }
}
</style>
