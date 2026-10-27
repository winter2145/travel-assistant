<template>
  <aside class="conv-sidebar">
    <div class="sidebar-header">
      <el-button type="primary" class="new-btn" @click="onNew">
        <el-icon><Plus /></el-icon>开启新旅程
      </el-button>
    </div>

    <div class="conv-list">
      <div class="conv-section-title"><span>最近的灵感</span><el-icon><MoreFilled /></el-icon></div>

      <div
        v-for="conv in store.conversations"
        :key="conv.id"
        class="conv-item"
        :class="{ active: store.activeId === conv.id }"
        @click="onSelect(conv.id)"
      >
        <el-icon class="conv-icon"><ChatDotRound /></el-icon>
        <div class="conv-info">
          <div class="conv-title">{{ conv.title }}</div>
          <div class="conv-time">{{ formatTime(conv.updatedAt) }}</div>
        </div>
        <el-button
          text
          circle
          size="small"
          class="conv-del"
          @click.stop="onRemove(conv.id)"
        >
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>

      <div v-if="store.conversations.length === 0" class="conv-empty">
        <span class="empty-spark">✦</span>
        <strong>旅程从一个念头开始</strong>
        <span>开启新旅程，记录你的旅行灵感</span>
      </div>
    </div>

    <div class="sidebar-footer">
      <div class="footer-avatar">旅</div>
      <div class="footer-copy"><strong>行旅规划师</strong><span>让每一程都有期待</span></div>
      <el-icon class="footer-more"><Setting /></el-icon>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { useConversationStore } from '@/stores/conversation'
import { useChatStore } from '@/stores/chat'
import { usePlanStore } from '@/stores/plan'
import { getPlanByConversation } from '@/api/plan'
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{ selected: [] }>()

const store = useConversationStore()
const chatStore = useChatStore()
const planStore = usePlanStore()

onMounted(() => {
  void store.refresh().catch(() => ElMessage.warning('暂时无法读取旅行会话，请稍后重试。'))
})

async function onNew() {
  try {
    await store.newConversation()
    planStore.clear()
  } catch {
    ElMessage.warning('暂时无法开启新旅程，请稍后重试。')
    return
  }
  chatStore.resetSession()
  emit('selected')
}

async function onSelect(id: string) {
  store.selectConversation(id)
  emit('selected')
  planStore.clear()
  try {
    await chatStore.loadMessages(id)
    const plan = await getPlanByConversation(id)
    if (plan.itinerary) planStore.loadPlanRecord(plan)
  } catch {
    // A conversation without a saved plan is a normal case.
  }
}

async function onRemove(id: string) {
  try {
    await store.removeConversation(id)
    if (store.activeId === null) {
      chatStore.resetSession()
      planStore.clear()
    }
  } catch {
    ElMessage.warning('删除会话失败，请稍后重试。')
  }
}

function formatTime(ts: number): string {
  const d = new Date(ts)
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  if (sameDay) {
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }
  return `${d.getMonth() + 1}月${d.getDate()}日`
}
</script>

<style scoped>
.conv-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fffefa;
}

.sidebar-header {
  padding: 16px 14px 14px;
  border-bottom: 1px solid #f0eee8;
}

.new-btn {
  width: 100%;
  height: 40px;
  border: 0;
  border-radius: 11px;
  background: #315f45;
  box-shadow: 0 5px 12px rgba(49,95,69,.14);
  font-weight: 600;
}
.new-btn:hover { background: #254e38; }

.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px 8px;
}

.conv-section-title { display: flex; align-items: center; justify-content: space-between; font-size: 11px; color: #9ba39c; font-weight: 600; letter-spacing: .3px; padding: 8px 10px 11px; }
.conv-section-title .el-icon { font-size: 15px; }

.conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: 11px;
  cursor: pointer;
  transition: background 0.15s;
}

.conv-item:hover {
  background: var(--bg-muted);
}

.conv-item.active {
  background: #f0f4ed;
}

.conv-icon {
  color: var(--text-faint);
  flex: 0 0 auto;
}

.conv-item.active .conv-icon {
  color: var(--brand);
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-title {
  font-size: 13px;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-time {
  font-size: 11px;
  color: var(--text-faint);
  margin-top: 1px;
}

.conv-del {
  opacity: 0;
  flex: 0 0 auto;
}

.conv-item:hover .conv-del {
  opacity: 1;
}

.conv-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  text-align: center;
  color: #a1a69f;
  font-size: 10px;
  padding: 52px 12px;
}
.conv-empty strong { color: #657166; font-size: 11px; font-weight: 600; }
.empty-spark { display: grid; place-items: center; width: 38px; height: 38px; margin-bottom: 4px; border-radius: 12px; background: #f2f3ec; color: #859879; font-size: 18px; }

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 13px 14px;
  border-top: 1px solid #f0eee8;
}
.footer-avatar { display: grid; place-items: center; width: 32px; height: 32px; border-radius: 10px; color: white; background: linear-gradient(145deg, #739374, #375c42); font-family: Georgia, serif; font-weight: 600; }
.footer-copy { display: grid; gap: 1px; flex: 1; }
.footer-copy strong { color: #455448; font-size: 11px; }
.footer-copy span { color: #a0a69f; font-size: 9px; }
.footer-more { color: #9ca49d; }
</style>
