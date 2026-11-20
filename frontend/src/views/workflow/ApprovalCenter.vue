<template>
  <div>
    <PageHeader title="审批中心" subtitle="平台统一审批引擎：多节点流转、跨模块回调业务状态">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-select v-model="query.module" placeholder="来源模块" clearable style="width: 130px" @change="search">
          <el-option v-for="m in ['OA', 'SRM', 'ERP', 'CRM']" :key="m" :label="m" :value="m" />
        </el-select>
        <el-select v-model="query.status" placeholder="审批状态" clearable style="width: 130px" @change="search">
          <el-option label="审批中" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-switch v-model="query.mine" active-text="仅看我提交" @change="search" />
        <div class="spacer"></div>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" size="default" empty-text="暂无审批记录">
        <el-table-column prop="title" label="审批事项" min-width="240" show-overflow-tooltip />
        <el-table-column prop="module" label="模块" width="80" align="center" />
        <el-table-column prop="bizType" label="业务类型" width="130" />
        <el-table-column prop="bizNo" label="业务单号" width="150" />
        <el-table-column prop="applicantName" label="申请人" width="95" />
        <el-table-column label="金额" width="115" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="审批进度" width="105" align="center">
          <template #default="{ row }">{{ row.currentStep }} / {{ row.totalSteps }}</template>
        </el-table-column>
        <el-table-column prop="currentNodeName" label="当前节点" width="120" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ dt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 14px">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <el-drawer v-model="drawer" title="审批单详情" size="560px">
      <div v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="事项" :span="2">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="模块">{{ detail.module }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.bizType }}</el-descriptions-item>
          <el-descriptions-item label="单号">{{ detail.bizNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detail.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ money(detail.amount) }}</el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :status="detail.status" /></el-descriptions-item>
          <el-descriptions-item label="说明" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="section-title" style="margin-top: 18px">审批节点</div>
        <el-timeline>
          <el-timeline-item
            v-for="node in detail.nodes || []"
            :key="node.id"
            :type="node.status === 'APPROVED' ? 'success' : node.status === 'REJECTED' ? 'danger' : node.status === 'PENDING' ? 'warning' : 'info'"
            :timestamp="node.actedAt ? dt(node.actedAt) : '待处理'"
          >
            <b>第 {{ node.stepNo }} 步 · {{ node.nodeName }}</b>
            <div class="muted" style="font-size: 12px">审批人：{{ node.approverName || '-' }} · 意见：{{ node.comment || '—' }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
      <template #footer>
        <el-button @click="drawer = false">关闭</el-button>
        <el-button v-if="detail?.status === 'PENDING'" type="danger" plain @click="act(false)">驳回</el-button>
        <el-button v-if="detail?.status === 'PENDING'" type="success" @click="act(true)">通过</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusTag from '../../components/StatusTag.vue'
import { workflowApi } from '../../api'
import { dt, money } from '../../utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const drawer = ref(false)
const detail = ref<any>(null)
const query = reactive({ page: 1, size: 10, module: '', status: '', mine: false })

async function load() {
  loading.value = true
  try {
    const data: any = await workflowApi.approvals({ ...query })
    rows.value = data.content || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

async function open(row: any) {
  detail.value = await workflowApi.detail(row.id)
  drawer.value = true
}

async function act(approve: boolean) {
  let comment = ''
  try {
    const res = await ElMessageBox.prompt(approve ? '审批意见（可留空）' : '请输入驳回原因', approve ? '审批通过' : '驳回审批', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputValue: approve ? '同意' : ''
    })
    comment = res.value || ''
  } catch {
    return
  }
  try {
    if (approve) await workflowApi.approve(detail.value.id, comment)
    else await workflowApi.reject(detail.value.id, comment)
    ElMessage.success('操作成功')
    drawer.value = false
    load()
  } catch {
    /* handled */
  }
}

onMounted(load)
</script>
