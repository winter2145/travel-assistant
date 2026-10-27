<template>
  <div class="workbench">
    <AppHeader />

    <!-- 移动端入口占据独立布局行，避免遮住聊天输入区 -->
    <div class="mobile-actions">
      <el-button round @click="sidebarOpen = true">
        <el-icon><ChatDotRound /></el-icon>会话
      </el-button>
      <el-button round @click="tripOpen = true">
        <el-icon><Compass /></el-icon>行程
      </el-button>
    </div>

    <div class="workbench-body">
      <!-- 左栏：会话列表（桌面） -->
      <div class="col-left">
        <ConversationSidebar />
      </div>

      <!-- 中栏：对话 -->
      <div class="col-center">
        <ChatPanel />
      </div>

      <!-- 右栏：旅行概览（桌面） -->
      <div class="col-right">
        <TripSidebar />
      </div>
    </div>

    <!-- 移动端抽屉：会话 -->
    <el-drawer v-model="sidebarOpen" title="会话" size="82%" :with-header="true" direction="ltr">
      <ConversationSidebar @selected="sidebarOpen = false" />
    </el-drawer>

    <!-- 移动端抽屉：行程概览 -->
    <el-drawer v-model="tripOpen" title="当前旅行" size="88%" direction="rtl">
      <TripSidebar />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/layouts/AppHeader.vue'
import ConversationSidebar from '@/components/sidebar/ConversationSidebar.vue'
import TripSidebar from '@/components/sidebar/TripSidebar.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'
import { useChatStore } from '@/stores/chat'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()

const sidebarOpen = ref(false)
const tripOpen = ref(false)

// 支持从行程详情"请求 AI 重新规划"跳转：自动发送携带的提示词
onMounted(() => {
  const prompt = route.query.prompt
  if (typeof prompt === 'string' && prompt.trim()) {
    void chatStore.sendMessage(prompt.trim())
    void router.replace({ query: {} })
  }
})
</script>

<style scoped>
.workbench {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.workbench-body {
  flex: 1;
  display: flex;
  min-height: 0;
  max-width: 1680px;
  width: 100%;
  margin: 0 auto;
  padding: 14px 16px 16px;
  gap: 14px;
}

.col-left {
  width: 248px;
  flex: 0 0 248px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 18px;
  box-shadow: 0 8px 28px rgba(38, 55, 42, 0.035);
}

.col-center {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 18px;
  box-shadow: 0 8px 28px rgba(38, 55, 42, 0.035);
}

.col-right {
  width: 318px;
  flex: 0 0 318px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 18px;
  box-shadow: 0 8px 28px rgba(38, 55, 42, 0.035);
}

/* 移动端适配：三栏折叠为抽屉 */
@media (max-width: 900px) {
  .workbench-body { padding: 8px; gap: 0; }
  .col-left {
    display: none;
  }
  .col-right {
    display: none;
  }
  .mobile-actions {
    display: flex;
  }
}

@media (max-width: 600px) {
  .workbench-body { padding: 0; }
  .col-center { border: 0; border-radius: 0; box-shadow: none; }
}

.mobile-actions {
  display: none;
  align-self: center;
  width: min(360px, calc(100% - 32px));
  padding: 4px;
  margin: 9px 16px 0;
  gap: 4px;
  flex: 0 0 auto;
  border: 1px solid #e8eae2;
  border-radius: 14px;
  background: #f1f3ed;
}

.mobile-actions :deep(.el-button) {
  flex: 1 1 0;
  height: 36px;
  margin: 0;
  border: 0;
  border-radius: 10px;
  box-shadow: none;
  background: transparent;
  color: #68756a;
  font-weight: 500;
}

.mobile-actions :deep(.el-button:hover) {
  background: #fffefa;
  color: var(--brand);
  box-shadow: 0 2px 8px rgba(38, 55, 42, 0.08);
}

@media (max-width: 900px) {
  .mobile-actions { display: flex; }
}

/* 抽屉内侧边栏高度撑满 */
:deep(.el-drawer__body) {
  padding: 0;
}

:deep(.el-drawer__body .conv-sidebar),
:deep(.el-drawer__body .trip-sidebar) {
  height: 100%;
}
</style>
