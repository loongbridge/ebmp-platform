<template>
  <div>
    <div class="topbar">
      <div class="topbar-row">
        <div>
          <div class="topbar-title">库存查询</div>
          <div class="topbar-sub">共 {{ list.length }} 条 · 预警 {{ alerts.length }} 条</div>
        </div>
        <button class="back-link" @click="load">刷新</button>
      </div>
    </div>

    <div class="page">
      <div class="field" style="margin-bottom: 10px">
        <input v-model="keyword" class="input" placeholder="搜索物料名称 / 编码 / 仓库" />
      </div>
      <div class="segmented">
        <button :class="{ active: !alertOnly }" @click="setAlert(false)">全部库存</button>
        <button :class="{ active: alertOnly }" @click="setAlert(true)">仅看预警</button>
      </div>

      <div v-if="loading" class="loading">加载中…</div>
      <div v-else-if="!filtered.length" class="empty">
        <div class="empty-icon">□</div>
        {{ alertOnly ? '暂无库存预警' : '未找到匹配的库存' }}
      </div>

      <div v-else class="list">
        <div v-for="s in filtered" :key="s.id" class="card">
          <div class="card-top">
            <div class="card-title">{{ s.materialName || '-' }}</div>
            <span v-if="s.alert" class="tag danger">库存预警</span>
            <span v-else class="tag ok">正常</span>
          </div>
          <div class="card-meta">
            <span>{{ s.materialCode }}</span>
            <span v-if="s.spec">规格 {{ s.spec }}</span>
            <span>{{ s.warehouseName || '-' }}</span>
          </div>
          <div class="card-meta">
            <span>库存 <b style="color: var(--dark)">{{ num(s.quantity) }}</b> {{ s.unit || '' }}</span>
            <span v-if="s.lockedQuantity">锁定 {{ num(s.lockedQuantity) }}</span>
            <span v-if="s.safeStock != null">安全库存 {{ num(s.safeStock) }}</span>
            <span v-if="s.costPrice">成本 {{ money(s.costPrice) }}</span>
          </div>
        </div>
      </div>

      <div v-if="toast" class="toast" :class="{ error: toastError }">{{ toast }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { erpApi } from '../api'
import { money } from '../utils/format'

const list = ref<any[]>([])
const alerts = ref<any[]>([])
const keyword = ref('')
const alertOnly = ref(false)
const loading = ref(false)
const toast = ref('')
const toastError = ref(false)

const filtered = computed(() => {
  const base = alertOnly.value ? alerts.value : list.value
  const k = keyword.value.trim().toLowerCase()
  if (!k) return base
  return base.filter((s) =>
    [s.materialName, s.materialCode, s.warehouseName, s.spec].some((v) => String(v || '').toLowerCase().includes(k))
  )
})

const num = (v: any) => Number(v || 0).toLocaleString('zh-CN')

function tip(msg: string, error = false) {
  toast.value = msg
  toastError.value = error
  setTimeout(() => (toast.value = ''), 2200)
}

async function load() {
  loading.value = true
  try {
    const [all, alertList] = await Promise.all([erpApi.stock({}), erpApi.alerts()])
    list.value = all as any
    alerts.value = alertList as any
  } catch (e: any) {
    tip(e.message || '加载失败', true)
  } finally {
    loading.value = false
  }
}

function setAlert(v: boolean) {
  alertOnly.value = v
}

onMounted(load)
</script>
