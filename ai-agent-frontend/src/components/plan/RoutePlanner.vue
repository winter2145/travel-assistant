<template>
  <div class="route-panel">
    <div class="route-form">
      <el-input v-model="origin" placeholder="起点：景点或地址" aria-label="路线起点" />
      <el-input v-model="destination" placeholder="终点：景点或地址" aria-label="路线终点" />
      <el-select v-model="mode" aria-label="出行方式">
        <el-option label="驾车" value="driving" />
        <el-option label="步行" value="walking" />
        <el-option label="公交" value="transit" />
      </el-select>
      <el-button type="primary" :loading="loading" :disabled="!origin.trim() || !destination.trim()" @click="search">查询路线</el-button>
    </div>
    <p class="route-hint">可直接输入景点名称、地址，或高德经纬度坐标；公交路线需填写所在城市。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <template v-if="result">
      <div v-if="pathSummary" class="route-summary">
        <span>{{ result.origin }} → {{ result.destination }}</span>
        <span>{{ distanceText }} · {{ durationText }}</span>
      </div>
      <div v-if="points.length > 1" class="route-map" role="img" :aria-label="`${result.origin}至${result.destination}的路线示意图`">
        <svg viewBox="0 0 600 150" preserveAspectRatio="none">
          <polyline :points="points" fill="none" stroke="#3b8a62" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" />
          <circle cx="12" cy="75" r="7" fill="#2e9d68" /><circle cx="588" cy="75" r="7" fill="#d66b54" />
        </svg>
        <div class="map-labels"><span>{{ result.origin }}</span><span>{{ result.destination }}</span></div>
      </div>
      <div v-if="steps.length" class="route-steps">
        <h4>路线指引</h4>
        <ol><li v-for="(step, index) in steps" :key="index">{{ step.instruction || step.road || '沿路线继续前行' }}<small v-if="step.distance">{{ formatDistance(step.distance) }}</small></li></ol>
      </div>
      <p v-else-if="!steps.length" class="route-summary">高德未返回可展示的路线步骤。</p>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { queryRoute, type RouteResult } from '@/api/route'

interface RouteStep { instruction: string; road?: string; distance?: string; polyline?: string }

const props = defineProps<{ city: string; defaultOrigin?: string; defaultDestination?: string }>()
const origin = ref(props.defaultOrigin ?? '')
const destination = ref(props.defaultDestination ?? '')
const mode = ref<RouteResult['mode']>('driving')
const result = ref<RouteResult | null>(null)
const loading = ref(false)
const error = ref('')
// /routes/query returns Amap's raw response as `route`; Amap nests the actual
// route payload one more level down (response.route.paths), as in its REST API.
const routeNode = computed(() => {
  const response = result.value?.route as Record<string, unknown> | undefined
  const nested = response?.route
  return (nested && typeof nested === 'object' ? nested : response) as Record<string, unknown> | undefined
})
const firstPath = computed(() => {
  const paths = routeNode.value?.paths
  return Array.isArray(paths) ? paths[0] as Record<string, unknown> | undefined : undefined
})
const firstTransit = computed(() => {
  const transits = routeNode.value?.transits
  return Array.isArray(transits) ? transits[0] as Record<string, unknown> | undefined : undefined
})
const pathSummary = computed(() => firstPath.value ?? firstTransit.value)
const distanceText = computed(() => pathSummary.value?.distance ? formatDistance(pathSummary.value.distance) : '')
const durationText = computed(() => pathSummary.value?.duration ? `${Math.max(1, Math.round(Number(pathSummary.value.duration) / 60))} 分钟` : '')
const steps = computed<RouteStep[]>(() => {
  if (Array.isArray(firstPath.value?.steps)) return (firstPath.value.steps as Record<string, string>[]).map((step) => ({
    instruction: step.instruction || step.road || '沿路线继续前行', distance: step.distance, polyline: step.polyline,
  }))
  const segments = firstTransit.value?.segments
  if (!Array.isArray(segments)) return []
  return segments.map((value) => {
    const segment = value as Record<string, unknown>
    const bus = segment.bus as { buslines?: Array<{ name?: string }> } | undefined
    const walking = segment.walking as { distance?: string; origin?: string; destination?: string } | undefined
    const lineNames = bus?.buslines?.map((line) => line.name).filter(Boolean)
    return {
      instruction: lineNames?.length ? `乘坐 ${lineNames.join(' / ')}` : `步行${walking?.origin && walking?.destination ? `：${walking.origin} → ${walking.destination}` : ''}`,
      distance: walking?.distance,
    }
  })
})
const points = computed(() => {
  const coordinates = steps.value.flatMap((step) => (step.polyline ?? '').split(';').filter(Boolean).map((point) => point.split(',').map(Number))).filter((pair) => pair.length === 2 && pair.every(Number.isFinite))
  if (coordinates.length < 2) return ''
  const xs = coordinates.map((point) => point[0]); const ys = coordinates.map((point) => point[1])
  const minX = Math.min(...xs), maxX = Math.max(...xs), minY = Math.min(...ys), maxY = Math.max(...ys)
  return coordinates.map(([x, y]) => `${12 + ((x - minX) / (maxX - minX || 1)) * 576},${12 + ((maxY - y) / (maxY - minY || 1)) * 126}`).join(' ')
})

async function search() {
  loading.value = true; error.value = ''; result.value = null
  try { result.value = await queryRoute({ origin: origin.value.trim(), destination: destination.value.trim(), city: props.city, mode: mode.value }) }
  catch (e) { error.value = (e as Error).message || '路线查询失败，请检查地点和地图服务配置。' }
  finally { loading.value = false }
}
function formatDistance(value: unknown) {
  const meters = Number(value)
  return Number.isFinite(meters) ? meters >= 1000 ? `${(meters / 1000).toFixed(1)} 公里` : `${Math.round(meters)} 米` : ''
}
</script>

<style scoped>
.route-panel { display: grid; gap: 12px; padding: 18px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 14px; }
.route-form { display: grid; grid-template-columns: 1fr 1fr 110px auto; gap: 8px; }
.route-hint { margin: -4px 0 0; color: var(--text-faint); font-size: 11px; }
.route-summary { display: flex; justify-content: space-between; gap: 10px; font-size: 13px; color: var(--text); }
.route-map { min-height: 120px; padding: 10px 12px; background: linear-gradient(180deg,#f2f7f0,#eaf3ed); border-radius: 10px; }
.route-map svg { width: 100%; height: 100px; overflow: visible; }
.map-labels { display: flex; justify-content: space-between; font-size: 11px; color: var(--text-secondary); }
.route-steps h4 { margin: 0 0 8px; font-size: 13px; }
.route-steps ol { display: grid; gap: 6px; padding-left: 20px; margin: 0; color: var(--text-secondary); font-size: 12px; }
.route-steps li small { margin-left: 8px; color: var(--text-faint); }
@media(max-width:700px) { .route-form { grid-template-columns: 1fr 1fr; } }
</style>
