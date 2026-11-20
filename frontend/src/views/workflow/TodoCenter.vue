<template>
  <div>
    <PageHeader title="统一待办中心" subtitle="汇总 OA 请假、SRM 采购、ERP 销售订单、CRM 合同等全部待审批事项">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="router.push('/workflow/approvals')">审批记录</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid" style="grid-template-columns: repeat(3, 1fr)">
      <div class="kpi-card">
        <div class="kpi-label">待我审批</div>
        <div class="kpi-value">{{ stats.pending || 0 }}<small>项</small></div>
        <div class="kpi-foot">需在当前节点处理</div>
        <el-icon class="kpi-icon"><Bell /></el-icon>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">我提交的审批</div>
        <div class="kpi-value">{{ stats.submitted || 0 }}<small>项</small></div>
        <div class="kpi-foot">全部历史提交</div>
        <el-icon class="kpi-icon"><Document /></el-icon>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">平台审批总量</div>
        <div class="kpi-value">{{ stats.total || 0 }}<small>项</small></div>
        <div class="kpi-foot">跨模块统一审批流</div>
        <el-icon class="kpi-icon"><Checked /></el-icon>
      </div>
    </div>

    <div class="page-card">
      <div class="section-title">待办审批列表</div>
      <el-table :data="list" v-loading="loading" size="default" empty-text="暂无待审批事项">
        <el-table-column prop="title" label="审批事项" min-width="230" show-overflow-tooltip />
        <el-table-column prop="module" label="来源模块" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.module }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="bizNo" label="业务单号" width="150" />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="nodeName" label="当前节点" width="130" />
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ dt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="success" @click="act(row, true)">通过</el-button>
            <el-button link type="danger" @click="act(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="detailVisible" title="审批详情" width="860px">
      <div v-if="detail">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="审批事项" :span="3">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ detail.bizType }}</el-descriptions-item>
          <el-descriptions-item label="业务单号">{{ detail.bizNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="来源模块">{{ detail.module }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detail.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ money(detail.amount) }}</el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :status="detail.status" /></el-descriptions-item>
          <el-descriptions-item label="当前节点">{{ detail.currentNodeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ dt(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="说明" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="section-title" style="margin-top: 18px">审批流转记录</div>
        <el-timeline>
          <el-timeline-item
            v-for="node in detail.nodes || []"
            :key="node.id"
            :type="node.status === 'APPROVED' ? 'success' : node.status === 'REJECTED' ? 'danger' : node.status === 'PENDING' ? 'warning' : 'info'"
            :timestamp="node.actedAt ? dt(node.actedAt) : '待处理'"
          >
            <b>第 {{ node.stepNo }} 步 · {{ node.nodeName }}</b>
            <div class="muted" style="font-size: 12px">审批人：{{ node.approverName || '-' }} · 状态：{{ node.status }} · 意见：{{ node.comment || '—' }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detail?.status === 'PENDING'" type="danger" plain @click="act(detail, false)">驳回</el-button>
        <el-button v-if="detail?.status === 'PENDING'" type="success" @click="act(detail, true)">审批通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusTag from '../../components/StatusTag.vue'
import { workflowApi } from '../../api'
import { dt, money } from '../../utils/format'

const router = useRouter()
const list = ref<any[]>([])
const stats = reactive<any>({ pending: 0, submitted: 0, total: 0 })
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const [todo, st] = await Promise.all([workflowApi.todo(), workflowApi.stats()])
    list.value = todo || []
    Object.assign(stats, st || {})
  } finally {
    loading.value = false
  }
}

async function openDetail(row: any) {
  detail.value = await workflowApi.detail(row.approvalId || row.id)
  detailVisible.value = true
}

async function act(row: any, approve: boolean) {
  const id = row.approvalId || row.id
  let comment = ''
  try {
    const result = await ElMessageBox.prompt(approve ? '请输入审批意见（可留空）' : '请输入驳回原因', approve ? '审批通过' : '驳回审批', {
      confirmButtonText: approve ? '确认通过' : '确认驳回',
      cancelButtonText: '取消',
      inputPlaceholder: approve ? '同意，请按计划执行' : '请说明驳回原因',
      inputValue: approve ? '同意' : ''
    })
    comment = result.value || ''
  } catch {
    return
  }
  try {
    if (approve) await workflowApi.approve(id, comment)
    else await workflowApi.reject(id, comment)
    ElMessage.success(approve ? '审批已通过' : '审批已驳回')
    detailVisible.value = false
    load()
  } catch {
    /* handled */
  }
}

onMounted(load)
</script>
