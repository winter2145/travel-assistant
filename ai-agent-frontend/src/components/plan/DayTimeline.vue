<template>
  <div class="day-timeline">
    <div class="day-header">
      <span class="day-badge">Day {{ day.day }}</span>
      <span class="day-date">{{ day.date }}</span>
      <span class="day-cost">活动费用 ¥{{ dayCost }}</span>
      <el-button text size="small" @click="emit('add')">
        <el-icon><Plus /></el-icon>添加活动
      </el-button>
    </div>

    <div class="timeline">
      <div
        v-for="(activity, index) in day.activities"
        :key="`${activity.name}-${index}`"
        class="timeline-item"
      >
        <div class="timeline-rail">
          <span class="dot"></span>
          <span v-if="index < day.activities.length - 1" class="line"></span>
        </div>
        <div class="timeline-content">
          <ActivityCard
            :activity="activity"
            :can-up="index > 0"
            :can-down="index < day.activities.length - 1"
            @move="(dir) => emit('move', { index, dir })"
            @edit="emit('edit', index)"
            @remove="emit('remove', index)"
          />
        </div>
      </div>

      <div v-if="day.activities.length === 0" class="day-empty">
        <span>这一天还没有安排活动</span>
        <el-button size="small" @click="emit('add')">添加第一个活动</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ItineraryDay } from '@/types/plan'
import ActivityCard from './ActivityCard.vue'

const props = defineProps<{ day: ItineraryDay }>()

const emit = defineEmits<{
  add: []
  edit: [index: number]
  remove: [index: number]
  move: [payload: { index: number; dir: number }]
}>()

const dayCost = computed(() =>
  props.day.activities.reduce((sum, a) => sum + (a.estimatedCost || 0), 0),
)
</script>

<style scoped>
.day-timeline {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.day-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.day-badge {
  background: var(--brand);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  border-radius: 6px;
  padding: 2px 10px;
}

.day-date {
  font-size: 13px;
  color: var(--text-secondary);
}

.day-cost {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-secondary);
}

.timeline {
  position: relative;
}

.timeline-item {
  display: flex;
  gap: 12px;
}

.timeline-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 0 0 14px;
  position: relative;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--brand);
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px var(--brand-soft);
  margin-top: 14px;
  z-index: 1;
}

.line {
  flex: 1;
  width: 2px;
  background: var(--border);
  min-height: 24px;
}

.timeline-content {
  flex: 1;
  min-width: 0;
  padding-bottom: 12px;
}

.day-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px;
  color: var(--text-faint);
  font-size: 13px;
  border: 1px dashed var(--border-strong);
  border-radius: 10px;
}
</style>
