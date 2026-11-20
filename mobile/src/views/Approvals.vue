<template>
  <div>
    <div class="topbar">
      <div class="topbar-row">
        <div>
          <div class="topbar-title">审批中心</div>
          <div class="topbar-sub">待我审批 {{ pendingCount }} 项 · 我发起 {{ stat.submitted || 0 }} 项</div>
        </div>
        <button class="back-link" @click="load">刷新</button>
      </div>
    </div>

    <div class="page">
      <div class="segmented">
        <button :class="{ active: tab === 'todo' }" @click="switchTab('todo')">待我审批</button>
        <button :class="{ active: tab === 'mine' }" @click="switchTab('mine')">我发起的</button>
      </div>

      <div v-if="loading" class="loading">加载中…</div>
      <div v-else-if="!list.length" class="empty">
        <div class="empty-icon">✓</div>
        {{ tab === 'todo' ? '暂无待审批单据' : '暂无我发起的单据' }}
      </div>

      <div v-else class="list">
        <div v-for="a in list" :key="a.approvalId || a.id" class="card" @click="open(a.approvalId || a.id)">
          <div class="card-top">
            <div class="card-title">{{ a.title }}</div>
            <span class="tag" :class="statusMeta(a.status || 'PENDING').cls">{{ statusMeta(a.status || 'PENDING').label }}</span>
          </div>
          <div class="card-meta">
            <span>{{ moduleName(a.module) }}</span>
            <span v-if="a.bizNo">{{ a.bizNo }}</span>
            <span>{{ a.applicantName || '-' }}</span>
            <span v-if="a.amount" class="amount">{{ money(a.amount) }}</span>
          </div>
          <div class="card-meta">
            <span>{{ a.nodeName || a.currentNodeName || '审批中' }}</span>
            <span>{{ dt(a.createdAt) }}</span>
          </div>
        </div>
      </div>

      <div v-if="toast" class="toast" :class="{ error: toastError }">{{ toast }}</div>
    </div>

    <div v-if="detail" class="mask" @click.self="detail = null">
      <div class="sheet">
        <div class="sheet-head">
          <div class="sheet-title">{{ detail.title }}</div>
          <button class="sheet-close" @click="detail = null">关闭</button>
        </div>

        <div class="detail-row"><span class="detail-key">单据编号</span><span class="detail-val">{{ detail.bizNo || '-' }}</span></div>
        <div class="detail-row"><span class="detail-key">所属模块</span><span class="detail-val">{{ moduleName(detail.module) }} / {{ bizName(detail.bizType) }}</span></div>
        <div class="detail-row"><span class="detail-key">申请人</span><span class="detail-val">{{ detail.applicantName || '-' }}</span></div>
        <div class="detail-row"><span class="detail-key">金额</span><span class="detail-val amount">{{ money(detail.amount) }}</span></div>
        <div class="detail-row"><span class="detail-key">当前节点</span><span class="detail-val">{{ detail.currentNodeName || '-' }}（{{ detail.currentStep }}/{{ detail.totalSteps }}）</span></div>
        <div class="detail-row"><span class="detail-key">状态</span><span class="detail-val"><span class="tag" :class="statusMeta(detail.status).cls">{{ statusMeta(detail.status).label }}</span></span></div>
        <div v-if="detail.remark" class="detail-row"><span class="detail-key">备注</span><span class="detail-val">{{ detail.remark }}</span></div>

        <div class="section">
          <div class="section-title" style="margin-bottom: 10px">审批流程</div>
          <div class="timeline">
            <div v-for="n in detail.nodes || []" :key="n.id" class="timeline-item" :class="{ done: n.status === 'APPROVED', rejected: n.status === 'REJECTED' }">
              <div class="timeline-name">{{ n.nodeName }} · {{ n.approverName || '待指派' }}</div>
              <div class="timeline-meta">
                {{ statusMeta(n.status).label }}<span v-if="n.actedAt"> · {{ dt(n.actedAt) }}</span>
                <div v-if="n.comment">{{ n.comment }}</div>
              </div>
            </div>
          </div>
        </div>

        <template v-if="canAct">
          <div class="field" style="margin-top: 6px">
            <div class="field-label">审批意见</div>
            <input v-model="comment" class="input" placeholder="选填，可填写审批意见" />
          </div>
          <div class="row-btns">
            <button class="btn ghost" :disabled="acting" @click="act(false)">驳回</button>
            <button class="btn" :disabled="acting" @click="act(true)">同意</button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { workflowApi } from '../api'
import { auth } from '../stores/auth'
import { dt, money, statusMeta } from '../utils/format'

const tab = ref<'todo' | 'mine'>('todo')
const list = ref<any[]>([])
const detail = ref<any>(null)
const stat = ref<any>({})
const loading = ref(false)
const acting = ref(false)
const comment = ref('')
const toast = ref('')
const toastError = ref(false)

// 顶部统计与当前页签无关，统一取后端统计值，避免切到“我发起的”后待审批数被清零
const pendingCount = computed(() =>
  typeof stat.value.pending === 'number' ? stat.value.pending : list.value.filter((a) => a.status === 'PENDING').length
)
const canAct = computed(() => {
  const d = detail.value
  if (!d || d.status !== 'PENDING') return false
  const user: any = auth.state.user || {}
  if ((user.roles || []).includes('ADMIN')) return true
  const node = (d.nodes || []).find((n: any) => n.stepNo === d.currentStep)
  return !!node && node.approverId === user.id && node.status === 'PENDING'
})

const MODULE: Record<string, string> = { OA: '办公', SRM: '采购', ERP: '销售', CRM: '合同' }
const BIZ: Record<string, string> = {
  LEAVE: '请假申请',
  PURCHASE_REQ: '采购需求',
  PURCHASE_ORDER: '采购订单',
  SALES_ORDER: '销售订单',
  CONTRACT: '合同审批'
}
const moduleName = (m?: string) => MODULE[m || ''] || m || '-'
const bizName = (b?: string) => BIZ[b || ''] || b || '-'

function tip(msg: string, error = false) {
  toast.value = msg
  toastError.value = error
  setTimeout(() => (toast.value = ''), 2200)
}

async function load() {
  loading.value = true
  detail.value = null
  try {
    if (tab.value === 'todo') {
      list.value = (await workflowApi.todo()) as any
    } else {
      const page: any = await workflowApi.list({ page: 1, size: 50, mine: true })
      list.value = page.list || page.content || []
    }
    stat.value = (await workflowApi.stats()) as any
  } catch (e: any) {
    tip(e.message || '加载失败', true)
  } finally {
    loading.value = false
  }
}

function switchTab(t: 'todo' | 'mine') {
  if (tab.value === t) return
  tab.value = t
  load()
}

async function open(id: number) {
  comment.value = ''
  try {
    detail.value = await workflowApi.detail(id)
  } catch (e: any) {
    tip(e.message || '加载详情失败', true)
  }
}

async function act(agree: boolean) {
  if (!detail.value) return
  acting.value = true
  try {
    if (agree) await workflowApi.approve(detail.value.id, comment.value)
    else await workflowApi.reject(detail.value.id, comment.value)
    tip(agree ? '审批已通过' : '已驳回')
    await load()
  } catch (e: any) {
    tip(e.message || '操作失败', true)
  } finally {
    acting.value = false
  }
}

onMounted(load)
</script>
