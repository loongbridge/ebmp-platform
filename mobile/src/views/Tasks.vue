<template>
  <div>
    <div class="topbar">
      <div class="topbar-row">
        <div>
          <div class="topbar-title">我的任务</div>
          <div class="topbar-sub">待办 {{ counts.TODO || 0 }} · 进行中 {{ counts.DOING || 0 }} · 已完成 {{ counts.DONE || 0 }}</div>
        </div>
        <button class="back-link" @click="load">刷新</button>
      </div>
    </div>

    <div class="page">
      <div class="segmented">
        <button v-for="s in tabs" :key="s.key" :class="{ active: status === s.key }" @click="switchStatus(s.key)">{{ s.name }}</button>
      </div>

      <div v-if="loading" class="loading">加载中…</div>
      <div v-else-if="!list.length" class="empty">
        <div class="empty-icon">☰</div>
        暂无任务
      </div>

      <div v-else class="list">
        <div v-for="t in list" :key="t.id" class="card">
          <div class="card-top">
            <div class="card-title">{{ t.title }}</div>
            <span class="tag" :class="priorityMeta(t.priority).cls">{{ priorityMeta(t.priority).label }}</span>
          </div>
          <div v-if="t.description" class="card-meta"><span>{{ t.description }}</span></div>
          <div class="card-meta">
            <span>截止 {{ d(t.dueDate) }}</span>
            <span>{{ statusMeta(t.status).label }}</span>
            <span>{{ t.creatorName || '-' }} 创建</span>
          </div>
          <div v-if="t.status !== 'DONE'" class="row-btns" style="margin-top: 10px">
            <button class="btn small" :disabled="busyId === t.id" @click="complete(t)">标记完成</button>
          </div>
          <div v-else class="card-meta"><span>完成于 {{ dt(t.completedAt) }}</span></div>
        </div>
      </div>

      <div v-if="toast" class="toast" :class="{ error: toastError }">{{ toast }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { oaApi } from '../api'
import { d, dt, priorityMeta, statusMeta } from '../utils/format'

const tabs = [
  { key: 'TODO', name: '待办' },
  { key: 'DOING', name: '进行中' },
  { key: 'DONE', name: '已完成' },
  { key: '', name: '全部' }
]

const status = ref('TODO')
const list = ref<any[]>([])
const all = ref<any[]>([])
const counts = reactive<any>({})
const loading = ref(false)
const busyId = ref<number | null>(null)
const toast = ref('')
const toastError = ref(false)

function tip(msg: string, error = false) {
  toast.value = msg
  toastError.value = error
  setTimeout(() => (toast.value = ''), 2200)
}

async function load() {
  loading.value = true
  try {
    all.value = (await oaApi.tasks({ mine: true })) as any
    counts.TODO = all.value.filter((t) => t.status === 'TODO').length
    counts.DOING = all.value.filter((t) => t.status === 'DOING').length
    counts.DONE = all.value.filter((t) => t.status === 'DONE').length
    list.value = status.value ? all.value.filter((t) => t.status === status.value) : all.value
  } catch (e: any) {
    tip(e.message || '加载失败', true)
  } finally {
    loading.value = false
  }
}

function switchStatus(s: string) {
  status.value = s
  list.value = s ? all.value.filter((t) => t.status === s) : all.value
}

async function complete(t: any) {
  busyId.value = t.id
  try {
    await oaApi.completeTask(t.id)
    tip('任务已完成')
    await load()
  } catch (e: any) {
    tip(e.message || '操作失败', true)
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>
