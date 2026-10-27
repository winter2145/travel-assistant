<template>
  <div class="message-input">
    <el-input
      v-model="draft"
      type="textarea"
      :rows="2"
      :maxlength="2000"
      resize="none"
      placeholder="描述你的旅行需求，例如：帮我规划大连 3 天 2 晚，预算 3000，情侣出行"
      :disabled="disabled"
      @keydown.enter.exact.prevent="submit"
    />
    <div class="input-footer">
      <span class="input-hint"><el-icon><MagicStick /></el-icon> 描述得越具体，行程越贴合你的想法</span>
      <el-button
        v-if="streaming"
        type="danger"
        plain
        size="small"
        @click="$emit('stop')"
      >
        <el-icon class="is-loading"><VideoPause /></el-icon>停止生成
      </el-button>
      <el-button
        v-else
        type="primary"
        size="small"
        :disabled="!canSend"
        @click="submit"
      >
        <el-icon><Promotion /></el-icon>发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{ streaming: boolean }>()
const emit = defineEmits<{ send: [text: string]; stop: [] }>()

const draft = ref('')

const canSend = computed(() => draft.value.trim().length > 0 && !props.streaming)
const disabled = computed(() => props.streaming)

function submit() {
  const text = draft.value.trim()
  if (!text || props.streaming) return
  emit('send', text)
  draft.value = ''
}

function setDraft(text: string) {
  draft.value = text
}

defineExpose({ setDraft })

// 停止生成后允许再次输入
watch(
  () => props.streaming,
  (v) => {
    if (!v) draft.value = draft.value // no-op，保持输入
  },
)
</script>

<style scoped>
.message-input {
  border-top: 1px solid #eae7df;
  padding: 14px clamp(14px, 3vw, 34px) 16px;
  background: rgba(255,255,255,.92);
}

.message-input :deep(.el-textarea__inner) {
  box-shadow: none;
  background: #f7f6f2;
  border: 1px solid #ece9e1;
  border-radius: 13px;
  padding: 12px 14px;
  transition: border-color .18s, box-shadow .18s;
  font-family: inherit;
}
.message-input :deep(.el-textarea__inner:focus) { border-color: #a8bba8; box-shadow: 0 0 0 3px rgba(83,120,87,.08); }

.input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.input-hint {
  font-size: 11px;
  color: #929991;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.input-hint .el-icon { color: #8a9b82; }
</style>
