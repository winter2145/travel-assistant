<template>
  <el-dialog
    :model-value="modelValue"
    :title="mode === 'add' ? '添加活动' : '编辑活动'"
    width="480px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form label-width="76px" label-position="left">
      <el-form-item label="活动名称" required>
        <el-input v-model="form.name" placeholder="如：星海广场" maxlength="40" />
      </el-form-item>
      <el-form-item label="地点">
        <el-input v-model="form.location" placeholder="地址或区域（可选）" maxlength="60" />
      </el-form-item>
      <div class="form-row">
        <el-form-item label="开始时间" required>
          <el-time-select v-model="form.startTime" start="06:00" step="00:30" end="23:00" placeholder="开始" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-time-select v-model="form.endTime" start="06:00" step="00:30" end="23:00" placeholder="结束" />
        </el-form-item>
      </div>
      <div class="form-row">
        <el-form-item label="费用(元)">
          <el-input-number v-model="form.estimatedCost" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="通勤(分)">
          <el-input-number v-model="form.travelMinutes" :min="0" :max="600" :controls="false" style="width: 100%" />
        </el-form-item>
      </div>
      <el-form-item label="备注">
        <el-input v-model="form.notes" type="textarea" :rows="2" maxlength="120" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :disabled="!form.name.trim() || !form.startTime || !form.endTime" @click="onConfirm">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { ItineraryActivity } from '@/types/plan'

interface ActivityForm {
  name: string
  location: string
  startTime: string
  endTime: string
  estimatedCost: number
  travelMinutes: number
  notes: string
}

const props = defineProps<{
  modelValue: boolean
  /** add: 新增；edit: 编辑现有活动 */
  mode: 'add' | 'edit'
  /** 编辑时传入的初始活动 */
  initial?: ItineraryActivity | null
}>()

const emit = defineEmits<{
  'update:modelValue': [v: boolean]
  confirm: [form: ActivityForm]
}>()

const emptyForm = (): ActivityForm => ({
  name: '',
  location: '',
  startTime: '09:00',
  endTime: '11:00',
  estimatedCost: 0,
  travelMinutes: 30,
  notes: '',
})

const form = reactive<ActivityForm>(emptyForm())
const errorMsg = ref('')

watch(
  () => [props.modelValue, props.mode, props.initial] as const,
  () => {
    errorMsg.value = ''
    if (props.modelValue && props.mode === 'edit' && props.initial) {
      Object.assign(form, {
        name: props.initial.name,
        location: props.initial.location ?? '',
        startTime: props.initial.startTime,
        endTime: props.initial.endTime,
        estimatedCost: props.initial.estimatedCost,
        travelMinutes: props.initial.travelMinutesFromPrevious ?? 30,
        notes: props.initial.notes ?? '',
      })
    } else if (props.modelValue) {
      Object.assign(form, emptyForm())
    }
  },
)

function onConfirm() {
  if (form.endTime <= form.startTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  errorMsg.value = ''
  emit('confirm', { ...form })
  emit('update:modelValue', false)
}
</script>

<style scoped>
.form-row {
  display: flex;
  gap: 12px;
}

.form-row .el-form-item {
  flex: 1;
}
</style>
