<template>
  <header class="app-header">
    <div class="header-left">
      <router-link to="/" class="brand">
        <span class="brand-mark"><img src="/favicon.svg" alt="" class="brand-logo" /></span>
        <span class="brand-name">行旅</span>
        <span class="brand-divider"></span>
        <span class="brand-sub">TRAVEL, THOUGHTFULLY PLANNED</span>
      </router-link>
    </div>
    <div class="header-right">
      <el-tooltip :content="backendStatusText" placement="bottom">
        <span class="backend-status" :class="backendOk ? 'is-ok' : 'is-down'">
          <span class="dot"></span>
          {{ backendOk ? '后端在线' : '后端未连接' }}
        </span>
      </el-tooltip>
      <el-button
        text
        :class="{ 'route-active': route.path === '/trips' || route.path === '/travel/history' }"
        @click="router.push('/travel/history')"
      >
        <el-icon><Van /></el-icon>
        <span class="nav-label">我的旅行</span>
      </el-button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { checkHealth } from '@/api/health'

const route = useRoute()
const router = useRouter()

const backendOk = ref(false)
const backendStatusText = ref('正在检测后端服务…')

let timer: number | undefined

async function probeHealth() {
  try {
    const res = await checkHealth()
    backendOk.value = res === 'ok'
    backendStatusText.value = backendOk.value ? '后端服务正常' : '健康检查返回异常'
  } catch {
    backendOk.value = false
    backendStatusText.value = '后端服务未启动（需先启动 ai-agent-backend 与 pexels MCP）'
  }
}

onMounted(() => {
  void probeHealth()
  timer = window.setInterval(() => void probeHealth(), 15000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--header-height);
  padding: 0 26px;
  background: rgba(255,255,255,.94);
  border-bottom: 1px solid #e9e6de;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
}

.brand {
  display: flex;
  align-items: center;
  gap: 9px;
}
.brand-mark { display: grid; place-items: center; width: 31px; height: 31px; border-radius: 10px; background: #edf2e9; }

.brand-logo {
  width: 22px;
  height: 22px;
  border-radius: 5px;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--brand);
}

.brand-sub {
  font-size: 9px;
  color: #a0a79f;
  letter-spacing: 1.1px;
}
.brand-divider { width: 1px; height: 15px; margin: 0 3px; background: #e8e7df; }

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}
.header-right :deep(.el-button) { height: 34px; padding: 0 12px; border-radius: 10px; color: #647267; }
.header-right :deep(.el-button:hover) { background: #f3f5f0; color: var(--brand); }

.backend-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  border: 1px solid #e7eae3;
  border-radius: 999px;
  padding: 5px 10px;
  background: #fbfcf9;
}

.backend-status .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.backend-status.is-ok .dot {
  background: var(--success);
}

.backend-status.is-down .dot {
  background: var(--danger);
}

.backend-status.is-ok {
  color: var(--success);
}

.backend-status.is-down {
  color: var(--danger);
}

.nav-label {
  margin-left: 2px;
}

.route-active {
  color: var(--brand) !important;
  font-weight: 600;
}

@media (max-width: 600px) {
  .brand-sub {
    display: none;
  }
  .brand-divider { display: none; }
  .nav-label {
    display: none;
  }
  .backend-status {
    font-size: 11px;
    padding: 2px 8px;
  }
}
</style>
