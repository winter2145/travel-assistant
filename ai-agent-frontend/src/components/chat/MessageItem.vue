<template>
  <div class="message-item" :class="[`is-${message.role}`, { 'is-error': message.status === 'error' }]">
    <div class="avatar" :class="message.role === 'user' ? 'avatar-user' : 'avatar-ai'">
      <el-icon v-if="message.role === 'user'"><User /></el-icon>
      <el-icon v-else><Guide /></el-icon>
    </div>

    <div class="message-body">
      <!-- 只展示对用户有意义的规划进度，不展示原始工具参数和返回数据 -->
      <div v-if="message.executionSteps?.length" class="planning-progress">
        <div class="progress-heading">
          <span class="progress-mark"><el-icon><Compass /></el-icon></span>
          <div>
            <strong>{{ message.status === 'streaming' ? '正在为你规划行程' : message.status === 'error' ? '规划暂时中断' : '本轮规划已完成' }}</strong>
            <span>{{ message.status === 'streaming' ? '我正在逐步整理可靠的旅行建议' : message.status === 'error' ? '可以稍后重新尝试' : '已为你整理好旅行建议' }}</span>
          </div>
          <el-icon v-if="message.status === 'streaming'" class="progress-spinner"><Loading /></el-icon>
          <el-icon v-else-if="message.status === 'error'" class="progress-failed"><CircleCloseFilled /></el-icon>
          <el-icon v-else class="progress-finished"><CircleCheckFilled /></el-icon>
        </div>
        <div class="progress-steps">
          <div v-for="step in message.executionSteps" :key="step.id" class="progress-step" :class="`is-${step.state}`">
            <span class="step-indicator">
              <el-icon v-if="step.state === 'done'"><Check /></el-icon>
              <el-icon v-else-if="step.state === 'error'"><Close /></el-icon>
              <span v-else class="step-dot"></span>
            </span>
            <span>{{ step.label }}</span>
            <small>{{ step.state === 'done' ? '已完成' : step.state === 'error' ? '未完成' : '进行中' }}</small>
          </div>
        </div>
        <p class="progress-footnote">{{ message.status === 'error' ? '请稍后重试，或调整需求后重新规划。' : '这里只展示规划进度。' }}</p>
      </div>

      <!-- 内容 -->
      <div v-if="message.content" class="content">
        <MarkdownRenderer :content="message.content" />
      </div>

      <!-- 流式光标 -->
      <span v-if="message.status === 'streaming' && !message.content" class="cursor-typing">
        <span class="dot"></span><span class="dot"></span><span class="dot"></span>
      </span>

      <!-- 错误 -->
      <div v-if="message.status === 'error' && message.error" class="error-box">
        <el-icon><WarningFilled /></el-icon>
        <span>{{ message.error }}</span>
      </div>

      <!-- 操作栏 -->
      <div v-if="message.role === 'assistant' && message.content && (message.status === 'done' || message.status === 'error')" class="actions">
        <el-button text size="small" @click="copyContent">
          <el-icon><CopyDocument /></el-icon>复制
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { ChatMessage } from '@/types/chat'
import MarkdownRenderer from './MarkdownRenderer.vue'

const props = defineProps<{ message: ChatMessage }>()

async function copyContent() {
  try {
    const content = props.message.content
    if (navigator.clipboard?.writeText) {
      try {
        await navigator.clipboard.writeText(content)
        ElMessage.success('已复制到剪贴板')
        return
      } catch {
        // Fall back for browsers that expose the API but block clipboard writes.
      }
    }

    const textarea = document.createElement('textarea')
    textarea.value = content
    textarea.setAttribute('readonly', '')
    textarea.style.position = 'fixed'
    textarea.style.top = '0'
    textarea.style.left = '-9999px'
    document.body.appendChild(textarea)

    let copied = false
    try {
      textarea.focus()
      textarea.select()
      textarea.setSelectionRange(0, textarea.value.length)
      copied = document.execCommand('copy')
    } finally {
      textarea.remove()
    }
    if (!copied) throw new Error('Clipboard copy command failed')

    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}
</script>

<style scoped>
.message-item {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
}

.message-item.is-user {
  flex-direction: row-reverse;
}

.avatar {
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.avatar-user {
  background: var(--brand);
  color: #fff;
}

.avatar-ai {
  background: var(--brand-soft);
  color: var(--brand);
}

.message-body {
  max-width: 86%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.is-user .message-body {
  align-items: flex-end;
}

.is-user .content {
  background: var(--brand);
  color: #fff;
  border-radius: 14px 14px 4px 14px;
  padding: 10px 14px;
}

.is-user .content :deep(p) {
  margin: 2px 0;
}

.content {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px 14px 14px 4px;
  padding: 12px 14px;
  width: fit-content;
  min-width: 40%;
  max-width: 100%;
}

.planning-progress {
  width: min(100%, 470px);
  padding: 15px 17px 12px;
  border: 1px solid #e3e8df;
  border-radius: 16px;
  background: linear-gradient(145deg, #fff 0%, #f8faf6 100%);
  box-shadow: 0 8px 22px rgba(43, 70, 50, 0.045);
}

.progress-heading {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-bottom: 14px;
}

.progress-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  color: #2b754f;
  background: #e8f3e9;
  border-radius: 12px;
  font-size: 18px;
}

.progress-heading > div { display: grid; gap: 2px; flex: 1; }
.progress-heading strong { color: #24352b; font-size: 13px; font-weight: 650; }
.progress-heading span:not(.progress-mark) { color: #8b948d; font-size: 11px; }
.progress-spinner { color: #43845e; animation: spin 1.6s linear infinite; }
.progress-finished { color: #35a566; font-size: 18px; }
.progress-failed { color: #c66d5c; font-size: 18px; }

.progress-steps { display: grid; gap: 10px; }
.progress-step { display: flex; align-items: center; gap: 9px; color: #526057; font-size: 12px; }
.step-indicator { display: grid; width: 17px; height: 17px; place-items: center; flex: 0 0 17px; border-radius: 50%; color: #fff; background: #36a767; font-size: 10px; }
.is-error .step-indicator { background: #c66d5c; }
.is-active .step-indicator { background: #fff; border: 1.5px solid #9aa59d; }
.step-dot { width: 6px; height: 6px; background: #6d7b71; border-radius: 50%; }
.progress-step small { margin-left: auto; color: #9ba59e; font-size: 10px; }
.progress-footnote { margin: 12px 0 0; padding-top: 9px; border-top: 1px solid #edf0eb; color: #9ba39d; font-size: 10px; }

@keyframes spin { to { transform: rotate(360deg); } }

@media (prefers-reduced-motion: reduce) {
  .progress-spinner { animation: none; }
}

.error-box {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--danger);
  background: rgba(201, 106, 90, 0.08);
  border-radius: 8px;
  padding: 8px 12px;
  width: fit-content;
}

.actions {
  display: flex;
  gap: 4px;
  padding-top: 2px;
}

.cursor-typing {
  display: inline-flex;
  gap: 4px;
  padding: 10px 4px;
}

.cursor-typing .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--border-strong);
  animation: blink 1.2s infinite ease-in-out;
}

.cursor-typing .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.cursor-typing .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.25;
  }
  40% {
    opacity: 1;
  }
}

@media (max-width: 600px) {
  .message-body {
    max-width: 100%;
  }
}
</style>
