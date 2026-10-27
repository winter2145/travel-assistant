<template>
  <div class="examples">
    <div class="examples-heading">
      <div><span class="examples-kicker">START WITH AN IDEA</span><div class="examples-title">从一个灵感开始</div></div>
      <span class="examples-caption">点选后可继续修改</span>
    </div>
    <div class="examples-grid">
      <button
        v-for="q in questions"
        :key="q.title"
        type="button"
        class="example-item"
        :disabled="disabled"
        @click="emit('pick', q.prompt)"
      >
        <span class="example-icon"><el-icon><component :is="q.icon" /></el-icon></span>
        <span class="example-copy"><strong>{{ q.title }}</strong><small>{{ q.text }}</small></span>
        <el-icon class="example-arrow"><ArrowUpRight /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
const emit = defineEmits<{ pick: [text: string] }>()
defineProps<{ disabled: boolean }>()

const questions: { title: string; text: string; prompt: string; icon: string }[] = [
  { title: '海滨慢游', text: '大连 3 天 · 情侣 · ¥3,000', prompt: '帮我规划大连 3 天 2 晚，预算 3000，情侣出行', icon: 'Location' },
  { title: '亲子出发', text: '青岛 4 天 · 亲子友好', prompt: '青岛 4 天亲子游，推荐适合孩子的景点和住宿', icon: 'School' },
  { title: '城市寻味', text: '成都 5 天 · 美食 · 慢节奏', prompt: '成都 5 天美食之旅，预算 4000，节奏不要太赶', icon: 'KnifeFork' },
  { title: '日出海岸', text: '预算 ¥2,000 内 · 看日出', prompt: '预算 2000 内想去海边看日出，哪里性价比高？', icon: 'Sunrise' },
]
</script>

<style scoped>
.examples {
  width: 100%;
  max-width: 780px;
  margin: 0 auto;
  padding: 4px 0 10px;
}

.examples-heading { display: flex; align-items: end; justify-content: space-between; margin-bottom: 11px; }
.examples-kicker { color: #a1a79f; font-size: 9px; letter-spacing: 1.3px; font-weight: 700; }
.examples-title { color: #35463a; font-family: Georgia, 'Songti SC', serif; font-size: 18px; margin-top: 1px; }
.examples-caption { color: #a0a59f; font-size: 10px; margin-bottom: 3px; }

.examples-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
}

.example-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  min-height: 62px;
  border: 1px solid #e9e6de;
  background: rgba(255,255,255,.84);
  border-radius: 13px;
  padding: 10px 12px;
  text-align: left;
  color: #35463a;
  cursor: pointer;
  transition: all 0.18s ease;
  font-family: inherit;
}

.example-item:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: #b8c7b6;
  background: #fff;
  box-shadow: 0 8px 18px rgba(48,74,51,.07);
}

.example-item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.example-icon { display: grid; width: 34px; height: 34px; flex: 0 0 34px; place-items: center; border-radius: 10px; color: #527659; background: #edf2e9; font-size: 16px; }
.example-copy { display: grid; min-width: 0; gap: 1px; flex: 1; }
.example-copy strong { color: #35463a; font-size: 12px; font-weight: 650; }
.example-copy small { overflow: hidden; color: #929991; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.example-arrow { color: #aeb7ac; font-size: 15px; }

@media (max-width: 600px) {
  .examples-grid { grid-template-columns: 1fr; gap: 7px; }
  .example-item { min-height: 54px; }
  .examples-caption { display: none; }
}
</style>
