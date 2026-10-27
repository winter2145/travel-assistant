<template>
  <div class="activity-card">
    <div class="ac-time">
      <span class="ac-start">{{ activity.startTime }}</span>
      <span class="ac-arrow">→</span>
      <span class="ac-end">{{ activity.endTime }}</span>
      <span class="ac-duration">{{ durationText }}</span>
    </div>

    <div class="ac-main">
      <div class="ac-name">
        {{ activity.name }}
        <span v-if="activity.estimatedCost > 0" class="ac-cost">¥{{ activity.estimatedCost }}</span>
        <span v-else class="ac-free">免费</span>
      </div>
      <div v-if="activity.location" class="ac-location">
        <el-icon><Location /></el-icon>{{ activity.location }}
      </div>
      <div v-if="activity.travelMinutesFromPrevious" class="ac-travel">
        <el-icon><Position /></el-icon>距上一站约 {{ activity.travelMinutesFromPrevious }} 分钟
      </div>
      <div v-if="activity.notes" class="ac-notes">{{ activity.notes }}</div>
    </div>

    <div class="ac-actions">
      <el-button text size="small" circle :disabled="!canUp" @click="emit('move', -1)">
        <el-icon><Top /></el-icon>
      </el-button>
      <el-button text size="small" circle :disabled="!canDown" @click="emit('move', 1)">
        <el-icon><Bottom /></el-icon>
      </el-button>
      <el-button text size="small" circle @click="emit('edit')">
        <el-icon><EditPen /></el-icon>
      </el-button>
      <el-button text size="small" circle type="danger" @click="emit('remove')">
        <el-icon><Delete /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ItineraryActivity } from '@/types/plan'

const props = defineProps<{
  activity: ItineraryActivity
  canUp: boolean
  canDown: boolean
}>()

const emit = defineEmits<{ move: [dir: number]; edit: []; remove: [] }>()

const durationText = computed(() => {
  const [sh, sm] = props.activity.startTime.split(':').map(Number)
  const [eh, em] = props.activity.endTime.split(':').map(Number)
  if (Number.isNaN(sh) || Number.isNaN(eh)) return ''
  const mins = (eh * 60 + em - (sh * 60 + sm))
  if (mins <= 0) return ''
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return h > 0 ? `${h}小时${m > 0 ? m + '分' : ''}` : `${m}分钟`
})
</script>

<style scoped>
.activity-card {
  display: flex;
  gap: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px 12px;
  align-items: flex-start;
}

.ac-time {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex: 0 0 64px;
  font-size: 12px;
  color: var(--brand);
  font-weight: 600;
  padding-top: 2px;
}

.ac-arrow {
  color: var(--border-strong);
  font-size: 10px;
}

.ac-duration {
  font-size: 10.5px;
  color: var(--text-faint);
  font-weight: 400;
}

.ac-main {
  flex: 1;
  min-width: 0;
}

.ac-name {
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.ac-cost {
  font-size: 12px;
  color: var(--warning);
  background: rgba(217, 154, 43, 0.1);
  border-radius: 5px;
  padding: 0 6px;
}

.ac-free {
  font-size: 11px;
  color: var(--success);
  background: var(--brand-softer);
  border-radius: 5px;
  padding: 0 6px;
}

.ac-location,
.ac-travel {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 3px;
}

.ac-location .el-icon,
.ac-travel .el-icon {
  font-size: 12px;
  color: var(--text-faint);
}

.ac-notes {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 3px;
  line-height: 1.5;
}

.ac-actions {
  display: flex;
  flex-direction: column;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
  flex: 0 0 auto;
}

.activity-card:hover .ac-actions {
  opacity: 1;
}

@media (max-width: 600px) {
  .ac-actions {
    opacity: 1;
  }
}
</style>
