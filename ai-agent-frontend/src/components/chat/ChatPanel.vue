<template>
  <div class="chat-panel">
    <!-- 空态：示例问题 -->
    <div v-if="store.messages.length === 0 && !store.lastError" class="chat-empty">
      <div class="chat-hero">
          <div class="hero-copy">
            <span class="hero-eyebrow"><i></i> YOUR PERSONAL TRAVEL DESIGNER</span>
            <h1>把想去的地方，<br /><em>变成一场好旅行。</em></h1>
            <p>告诉我目的地、天数、预算和偏好，<br class="desktop-break" />一起把灵感整理成真正可走的行程。</p>
            <div class="hero-points">
              <span><el-icon><MapLocation /></el-icon>按兴趣挑选目的地</span>
              <span><el-icon><Clock /></el-icon>每天节奏更从容</span>
              <span><el-icon><Coin /></el-icon>预算安排更清晰</span>
            </div>
          </div>
          <div class="hero-art" aria-hidden="true">
            <div class="sun"></div>
            <div class="hill hill-back"></div>
            <div class="hill hill-front"></div>
            <div class="art-label"><span>下一站</span><strong>由你决定</strong><i>✦</i></div>
            <div class="art-route"><span></span><b>✦</b><span></span></div>
          </div>
      </div>
      <ExampleQuestions :disabled="store.isStreaming" @pick="onPick" />
    </div>

    <!-- 错误态（首条发送失败） -->
    <div v-else-if="store.messages.length === 0 && store.lastError" class="chat-empty">
      <div class="chat-hero chat-error-hero">
        <el-icon class="hero-error"><WarningFilled /></el-icon>
        <h3>对话暂时不可用</h3>
        <p class="error-text">{{ store.lastError.message }}</p>
        <el-button type="primary" @click="store.resetSession()">
          <el-icon><Refresh /></el-icon>重试
        </el-button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div v-else ref="listRef" class="message-list">
      <MessageItem v-for="msg in store.messages" :key="msg.id" :message="msg" />
    </div>

    <!-- 输入区 -->
    <MessageInput
      ref="inputRef"
      class="input-wrap"
      :streaming="store.isStreaming"
      @send="onSend"
      @stop="store.stopStreaming()"
    />
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useChatStore } from '@/stores/chat'
import MessageItem from './MessageItem.vue'
import MessageInput from './MessageInput.vue'
import ExampleQuestions from './ExampleQuestions.vue'

const store = useChatStore()
const listRef = ref<HTMLElement | null>(null)
const inputRef = ref<InstanceType<typeof MessageInput> | null>(null)

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

function onSend(text: string) {
  void store.sendMessage(text)
  void scrollToBottom()
}

function onPick(text: string) {
  inputRef.value?.setDraft(text)
}

// 流式输出时持续滚动到底部
watch(
  () => store.messages.map((m) => m.content).join('|'),
  () => void scrollToBottom(),
)
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  background: #fbfaf7;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow-y: auto;
  padding: 24px clamp(24px, 5vw, 70px) 16px;
}

.chat-hero {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 260px;
  max-width: 780px;
  width: 100%;
  margin: 0 auto 22px;
  padding: 24px 0 8px;
}

.hero-copy { position: relative; z-index: 1; max-width: 520px; }
.hero-eyebrow { display: inline-flex; align-items: center; gap: 8px; color: #718274; font-size: 10px; font-weight: 700; letter-spacing: 1.5px; }
.hero-eyebrow i { width: 7px; height: 7px; border-radius: 50%; background: #d08d57; }
.chat-hero h1 { margin: 17px 0 12px; color: #25382d; font-family: Georgia, 'Songti SC', serif; font-size: clamp(31px, 3.2vw, 43px); font-weight: 500; line-height: 1.2; letter-spacing: -1.5px; }
.chat-hero h3 { margin: 0 0 6px; color: var(--text); font-size: 17px; }
.chat-hero h1 em { color: #5d7c62; font-style: normal; }
.chat-hero p { margin: 0; color: #79827a; font-size: 13px; line-height: 1.9; }
.hero-points { display: flex; flex-wrap: wrap; gap: 8px 16px; margin-top: 19px; }
.hero-points span { display: inline-flex; align-items: center; gap: 5px; color: #69776c; font-size: 11px; }
.hero-points .el-icon { color: #718c73; font-size: 14px; }

.hero-art { position: absolute; right: 6px; top: 24px; width: 235px; height: 225px; overflow: hidden; border-radius: 47% 47% 18px 18px; background: linear-gradient(160deg, #e8eee5 0%, #edf0e5 52%, #dce5d9 100%); }
.sun { position: absolute; right: 35px; top: 27px; width: 56px; height: 56px; border-radius: 50%; background: #e6b57e; box-shadow: 0 0 0 11px rgba(230,181,126,.12); }
.hill { position: absolute; bottom: -52px; border-radius: 50% 50% 0 0; transform: rotate(-10deg); }
.hill-back { left: -38px; width: 250px; height: 155px; background: #a7bba2; }
.hill-front { right: -70px; bottom: -76px; width: 260px; height: 178px; background: #617d67; transform: rotate(14deg); }
.art-label { position: absolute; left: 22px; top: 73px; display: grid; gap: 2px; color: #fff; text-shadow: 0 1px 8px rgba(30,55,36,.18); }
.art-label span { font-size: 10px; letter-spacing: 1px; opacity: .85; }
.art-label strong { font-family: Georgia, 'Songti SC', serif; font-size: 21px; font-weight: 500; }
.art-label i { position: absolute; left: 84px; top: 4px; color: #f2d29d; font-size: 15px; font-style: normal; }
.art-route { position: absolute; left: 26px; right: 20px; bottom: 33px; display: flex; align-items: center; justify-content: space-between; color: #f3d9ac; }
.art-route span { width: 6px; height: 6px; background: #f3d9ac; border-radius: 50%; }
.art-route span:first-child { box-shadow: 32px -12px 0 -1px rgba(255,255,255,.65), 69px -18px 0 -1px rgba(255,255,255,.65); }
.art-route b { font-size: 15px; }

.chat-hero .hero-error {
  font-size: 40px;
}

.hero-error {
  color: var(--danger);
  margin-bottom: 12px;
}
.chat-error-hero { display: flex; flex-direction: column; justify-content: center; text-align: center; }

.error-text {
  margin-bottom: 14px !important;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px clamp(18px, 4vw, 56px);
}

.input-wrap {
  flex: 0 0 auto;
}

@media (max-width: 1100px) {
  .hero-art { opacity: .42; right: 0; }
  .hero-copy { max-width: 80%; }
}
@media (max-width: 600px) {
  .chat-empty { padding: 22px 18px 14px; }
  .chat-hero { min-height: 290px; align-items: flex-start; padding-top: 24px; margin-bottom: 10px; }
  .hero-copy { max-width: 100%; }
  .hero-art { width: 160px; height: 150px; top: auto; bottom: 0; right: 0; opacity: .32; }
  .desktop-break { display: none; }
  .hero-points { gap: 7px 12px; }
  .message-list { padding: 14px 12px 90px; }
}
</style>
