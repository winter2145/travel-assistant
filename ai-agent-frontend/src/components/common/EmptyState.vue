<template>
  <div class="empty-state">
    <el-icon><component :is="icon" /></el-icon>
    <h4>{{ title }}</h4>
    <p>{{ description }}</p>
    <div v-if="$slots.default" class="empty-action">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon?: string
  title: string
  description?: string
}>()
</script>

<style scoped>
.empty-action {
  margin-top: 14px;
}
</style>
