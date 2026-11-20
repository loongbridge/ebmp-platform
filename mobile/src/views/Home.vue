<template>
  <div>
    <div class="topbar">
      <div class="topbar-row">
        <div>
          <div class="topbar-title">{{ greeting }}，{{ auth.displayName.value || '同事' }}</div>
          <div class="topbar-sub">{{ today }} · 企业商业管理平台</div>
        </div>
        <router-link to="/notices" class="back-link">公告</router-link>
      </div>
    </div>

    <div class="page">
      <div class="kpi-wrap">
        <div class="kpi">
          <div class="kpi-label">本月销售额</div>
          <div class="kpi-value small">{{ moneyShort(kpi.monthSales) }}</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">待我审批</div>
          <div class="kpi-value">{{ todos.approvals?.length || 0 }}<span style="font-size: 12px; color: var(--muted)"> 项</span></div>
        </div>
        <div class="kpi">
          <div class="kpi-label">库存预警</div>
          <div class="kpi-value">{{ kpi.stockAlerts || 0 }}<span style="font-size: 12px; color: var(--muted)"> 项</span></div>
        </div>
        <div class="kpi">
          <div class="kpi-label">商机总额</div>
          <div class="kpi-value small">{{ moneyShort(kpi.crmPipeline) }}</div>
        </div>
      </div>

      <div class="section">
        <div class="section-head">
          <div class="section-title">快捷入口</div>
        </div>
        <div class="quick-grid">
          <router-link v-for="q in quicks" :key="q.path" :to="q.path" class="quick-item">
            <span class="quick-icon">
              <svg viewBox="0 0 24 24" width="20" height="20">
                <path :d="q.icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round" />
              </svg>
            </span>
            <span>{{ q.name }}</span>
          </router-link>
        </div>
      </div>

      <div class="section">
        <div class="section-head">
          <div class="section-title">待办审批</div>
          <router-link class="section-more" to="/approvals">全部 {{ todos.approvals?.length || 0 }} 项 ›</router-link>
        </div>
        <div class="list">
          <div v-if="!todos.approvals?.length" class="card empty" style="padding: 26px">暂无待审批单据</div>
          <router-link v-for="a in todos.approvals?.slice(0, 3)" :key="a.approvalId" class="card" to="/approvals">
            <div class="card-top">
              <div class="card-title">{{ a.title }}</div>
              <span class="tag warn">待审批</span>
            </div>
            <div class="card-meta">
              <span>{{ a.applicantName || '-' }}</span>
              <span v-if="a.amount">金额 {{ money(a.amount) }}</span>
              <span>{{ a.nodeName || a.bizType }}</span>
            </div>
          </router-link>
        </div>
      </div>

      <div class="section">
        <div class="section-head">
          <div class="section-title">我的任务</div>
          <router-link class="section-more" to="/tasks">全部 {{ todos.tasks?.length || 0 }} 项 ›</router-link>
        </div>
        <div class="list">
          <div v-if="!todos.tasks?.length" class="card empty" style="padding: 26px">暂无待办任务</div>
          <router-link v-for="t in todos.tasks?.slice(0, 3)" :key="t.id" class="card" to="/tasks">
            <div class="card-top">
              <div class="card-title">{{ t.title }}</div>
              <span class="tag" :class="priorityMeta(t.priority).cls">{{ priorityMeta(t.priority).label }}</span>
            </div>
            <div class="card-meta">
              <span>截止 {{ d(t.dueDate) }}</span>
              <span>{{ statusMeta(t.status).label }}</span>
            </div>
          </router-link>
        </div>
      </div>

      <div class="section">
        <div class="section-head">
          <div class="section-title">公司公告</div>
          <router-link class="section-more" to="/notices">更多 ›</router-link>
        </div>
        <div class="list">
          <div v-if="!notices.length" class="card empty" style="padding: 26px">暂无公告</div>
          <router-link v-for="n in notices" :key="n.id" class="card" to="/notices">
            <div class="card-top">
              <div class="card-title">{{ n.title }}</div>
              <span v-if="n.category" class="tag">{{ n.category }}</span>
            </div>
            <div class="card-meta">
              <span>{{ n.publisherName || '管理员' }}</span>
              <span>{{ dt(n.publishTime) }}</span>
            </div>
          </router-link>
        </div>
      </div>

      <div v-if="toast" class="toast" :class="{ error: toastError }">{{ toast }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { dashboardApi, oaApi } from '../api'
import { auth } from '../stores/auth'
import { d, dt, money, moneyShort, priorityMeta, statusMeta } from '../utils/format'

const kpi = reactive<any>({})
const todos = reactive<any>({ approvals: [], tasks: [], total: 0 })
const notices = ref<any[]>([])
const toast = ref('')
const toastError = ref(false)

const today = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const quicks = [
  { path: '/approvals', name: '审批', icon: 'M8 4h8v3H8zM6 4H5a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-1M7 13l3 3 7-7' },
  { path: '/tasks', name: '任务', icon: 'M4 6h10M4 12h10M4 18h6M18 5l2 2 3-3' },
  { path: '/stock', name: '库存', icon: 'M3 7l9-4 9 4-9 4zM3 7v10l9 4 9-4V7M12 11v10' },
  { path: '/leads', name: '线索', icon: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21c0-4 3.6-6 8-6s8 2 8 6' },
  { path: '/notices', name: '公告', icon: 'M4 5h16v14H4zM7 9h10M7 12h10M7 15h6' },
  { path: '/me', name: '打卡', icon: 'M12 7v5l3 2M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z' },
  { path: '/me', name: '设置', icon: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19 12a7 7 0 0 0-.1-1l2-1.6-2-3.4-2.3 1a7 7 0 0 0-1.7-1L14.5 3h-5L9 6a7 7 0 0 0-1.7 1L5 6l-2 3.4L5 11a7 7 0 0 0 0 2l-2 1.6 2 3.4 2.3-1a7 7 0 0 0 1.7 1L9.5 21h5l.4-3a7 7 0 0 0 1.7-1L19 18l2-3.4L19 13a7 7 0 0 0 .1-1z' },
  { path: '/notices', name: '待阅', icon: 'M3 6h18v12H3zM3 7l9 6 9-6' }
]

function tip(msg: string, error = false) {
  toast.value = msg
  toastError.value = error
  setTimeout(() => (toast.value = ''), 2200)
}

async function load() {
  try {
    auth.refresh()
    const overview: any = await dashboardApi.overview()
    Object.assign(kpi, overview.kpi || {})
    notices.value = (overview.notices || []).slice(0, 2)
    const t: any = await dashboardApi.todos()
    todos.approvals = t.approvals || []
    todos.tasks = t.tasks || []
    todos.total = t.total || 0
  } catch (e: any) {
    tip(e.message || '加载失败', true)
  }
}

onMounted(load)
</script>
