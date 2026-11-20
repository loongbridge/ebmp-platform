<template>
  <div>
    <div class="topbar">
      <div class="topbar-row">
        <div>
          <div class="topbar-title">销售线索</div>
          <div class="topbar-sub">线索 {{ stat.leadCount || 0 }} · 新线索 {{ stat.newLeadCount || 0 }} · 赢单率 {{ stat.winRate || 0 }}%</div>
        </div>
        <button class="back-link" @click="load">刷新</button>
      </div>
    </div>

    <div class="page">
      <div class="pull-card" style="margin-top: -24px">
        <div style="display: flex; justify-content: space-between">
          <div>
            <div class="kpi-label">商机总额</div>
            <div class="kpi-value small">{{ moneyShort(stat.openAmount) }}</div>
          </div>
          <div style="text-align: right">
            <div class="kpi-label">赢单金额</div>
            <div class="kpi-value small">{{ moneyShort(stat.wonAmount) }}</div>
          </div>
        </div>
      </div>

      <div class="field" style="margin-top: 14px; margin-bottom: 10px">
        <input v-model="keyword" class="input" placeholder="搜索线索名称 / 公司 / 联系人" />
      </div>

      <div class="segmented">
        <button :class="{ active: status === '' }" @click="status = ''">全部</button>
        <button :class="{ active: status === 'NEW' }" @click="status = 'NEW'">新线索</button>
        <button :class="{ active: status === 'FOLLOWING' }" @click="status = 'FOLLOWING'">跟进中</button>
        <button :class="{ active: status === 'CONVERTED' }" @click="status = 'CONVERTED'">已转化</button>
      </div>

      <div v-if="loading" class="loading">加载中…</div>
      <div v-else-if="!filtered.length" class="empty">
        <div class="empty-icon">◎</div>
        暂无线索
      </div>

      <div v-else class="list">
        <div v-for="l in filtered" :key="l.id" class="card" @click="open(l)">
          <div class="card-top">
            <div class="card-title">{{ l.name }}</div>
            <span class="tag" :class="statusMeta(l.status).cls">{{ statusMeta(l.status).label }}</span>
          </div>
          <div class="card-meta">
            <span v-if="l.company">{{ l.company }}</span>
            <span v-if="l.contactName">{{ l.contactName }}</span>
            <span v-if="l.source">来源 {{ l.source }}</span>
          </div>
          <div class="card-meta">
            <span>{{ l.leadNo }}</span>
            <span>负责人 {{ l.ownerName || '-' }}</span>
            <span v-if="l.nextFollowAt">下次跟进 {{ dt(l.nextFollowAt) }}</span>
          </div>
        </div>
      </div>

      <div v-if="toast" class="toast" :class="{ error: toastError }">{{ toast }}</div>
    </div>

    <div v-if="current" class="mask" @click.self="current = null">
      <div class="sheet">
        <div class="sheet-head">
          <div class="sheet-title">{{ current.name }}</div>
          <button class="sheet-close" @click="current = null">关闭</button>
        </div>
        <div class="detail-row"><span class="detail-key">线索编号</span><span class="detail-val">{{ current.leadNo }}</span></div>
        <div class="detail-row"><span class="detail-key">状态</span><span class="detail-val"><span class="tag" :class="statusMeta(current.status).cls">{{ statusMeta(current.status).label }}</span></span></div>
        <div class="detail-row"><span class="detail-key">公司</span><span class="detail-val">{{ current.company || '-' }}</span></div>
        <div class="detail-row"><span class="detail-key">联系人</span><span class="detail-val">{{ current.contactName || '-' }}</span></div>
        <div class="detail-row"><span class="detail-key">电话</span><span class="detail-val">{{ current.phone || '-' }}</span></div>
        <div class="detail-row"><span class="detail-key">邮箱</span><span class="detail-val">{{ current.email || '-' }}</span></div>
        <div class="detail-row"><span class="detail-key">来源</span><span class="detail-val">{{ current.source || '-' }}</span></div>
        <div class="detail-row"><span class="detail-key">负责人</span><span class="detail-val">{{ current.ownerName || '-' }}</span></div>
        <div class="detail-row"><span class="detail-key">下次跟进</span><span class="detail-val">{{ dt(current.nextFollowAt) }}</span></div>
        <div class="detail-row"><span class="detail-key">备注</span><span class="detail-val">{{ current.remark || '-' }}</span></div>
        <div class="row-btns">
          <a v-if="current.phone" class="btn ghost" style="display: flex; align-items: center; justify-content: center" :href="'tel:' + current.phone">拨打电话</a>
          <button v-if="current.status !== 'CONVERTED'" class="btn" :disabled="converting" @click="convert">
            {{ confirmStep ? '再次点击确认转化' : '转为商机' }}
          </button>
        </div>
        <div v-if="current.status === 'CONVERTED'" class="hint" style="text-align: center">该线索已转化，无需重复操作</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { crmApi } from '../api'
import { dt, moneyShort, statusMeta } from '../utils/format'

const list = ref<any[]>([])
const stat = reactive<any>({})
const keyword = ref('')
const status = ref('')
const loading = ref(false)
const current = ref<any>(null)
const converting = ref(false)
const confirmStep = ref(false)
const toast = ref('')
const toastError = ref(false)

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return list.value.filter((l) => {
    if (status.value && l.status !== status.value) return false
    if (!k) return true
    return [l.name, l.company, l.contactName, l.phone].some((v) => String(v || '').toLowerCase().includes(k))
  })
})

function tip(msg: string, error = false) {
  toast.value = msg
  toastError.value = error
  setTimeout(() => (toast.value = ''), 2200)
}

async function load() {
  loading.value = true
  try {
    const [leads, stats] = await Promise.all([crmApi.leads(), crmApi.stats()])
    list.value = leads as any
    Object.assign(stat, stats)
  } catch (e: any) {
    tip(e.message || '加载失败', true)
  } finally {
    loading.value = false
  }
}

function open(l: any) {
  current.value = l
  confirmStep.value = false
}

async function convert() {
  if (!current.value) return
  if (!confirmStep.value) {
    confirmStep.value = true
    return
  }
  converting.value = true
  try {
    await crmApi.convertLead(current.value.id)
    tip('已成功转化为商机')
    current.value = null
    confirmStep.value = false
    await load()
  } catch (e: any) {
    tip(e.message || '转化失败', true)
  } finally {
    converting.value = false
  }
}

onMounted(load)
</script>
