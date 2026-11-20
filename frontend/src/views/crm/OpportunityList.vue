<template>
  <div>
    <PageHeader title="商机管理" subtitle="商机漏斗与赢单转化：赢单自动生成销售订单草稿，打通 CRM 与进销存">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建商机</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">在跟商机金额</div>
        <div class="kpi-value" style="font-size: 21px">{{ money(stats.openAmount) }}</div>
        <div class="kpi-foot">OPEN 状态商机合计</div>
        <el-icon class="kpi-icon"><TrendCharts /></el-icon>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">累计赢单金额</div>
        <div class="kpi-value" style="font-size: 21px">{{ money(stats.wonAmount) }}</div>
        <div class="kpi-foot">WON 状态商机合计</div>
        <el-icon class="kpi-icon"><Trophy /></el-icon>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">商机总数</div>
        <div class="kpi-value">{{ stats.opportunityCount || 0 }}<small>个</small></div>
        <div class="kpi-foot">覆盖客户 {{ stats.customerCount || 0 }} 家</div>
        <el-icon class="kpi-icon"><DataLine /></el-icon>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">赢单率</div>
        <div class="kpi-value">{{ stats.winRate || 0 }}<small>%</small></div>
        <div class="kpi-foot">赢单 /（赢单 + 输单）</div>
        <el-icon class="kpi-icon"><Aim /></el-icon>
      </div>
    </div>

    <div class="page-card" style="margin-bottom: 16px">
      <div class="section-title">商机漏斗看板</div>
      <div class="pipeline-board">
        <div v-for="col in pipeline" :key="col.stage" class="pipeline-col">
          <div class="pipeline-col-head">
            <b>{{ col.stageName }}</b>
            <span>{{ col.count }} 个 · {{ money(col.amount) }}</span>
          </div>
          <div v-for="opp in col.opportunities" :key="opp.id" class="pipeline-card" @click="openDetail(opp)">
            <b>{{ opp.name }}</b>
            <div class="p-amount">{{ money(opp.amount) }}</div>
            <div class="muted" style="margin-top: 4px; font-size: 11px">赢率 {{ opp.probability }}% · {{ d(opp.expectedCloseDate) }}</div>
          </div>
          <div v-if="col.opportunities.length === 0" class="empty-box" style="padding: 14px 0">暂无商机</div>
        </div>
      </div>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索商机 / 客户" clearable style="width: 240px" />
        <el-select v-model="stageFilter" placeholder="阶段" clearable style="width: 140px">
          <el-option v-for="s in stages" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ filtered.length }} 个商机</el-tag>
      </div>
      <el-table :data="filtered" v-loading="loading" size="default">
        <el-table-column prop="oppNo" label="商机编号" width="135" />
        <el-table-column prop="name" label="商机名称" min-width="230" show-overflow-tooltip>
          <template #default="{ row }"><span class="link-text" @click="openDetail(row)">{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" min-width="170" show-overflow-tooltip />
        <el-table-column label="阶段" width="100" align="center">
          <template #default="{ row }"><StatusTag :status="row.stage" /></template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="probability" label="赢率" width="75" align="center">
          <template #default="{ row }">{{ row.probability }}%</template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="90" />
        <el-table-column label="预计成交" width="115">
          <template #default="{ row }">{{ d(row.expectedCloseDate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'OPEN'" link type="success" @click="openWin(row)">赢单</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑商机' : '新建商机'" width="620px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商机名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="客户">
          <el-select v-model="form.customerId" filterable style="width: 100%" placeholder="选择客户">
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="销售阶段">
          <el-select v-model="form.stage" style="width: 100%">
            <el-option v-for="s in stages.filter((x) => !['WON', 'LOST'].includes(x.value))" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="商机金额">
          <el-input-number v-model="form.amount" :min="0" :step="10000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="赢单概率">
          <el-slider v-model="form.probability" :max="100" :step="5" show-input />
        </el-form-item>
        <el-form-item label="预计成交">
          <el-date-picker v-model="form.expectedCloseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.ownerId" filterable style="width: 100%">
            <el-option v-for="u in users" :key="u.id" :label="`${u.name}（${u.deptName || '-'}）`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="winVisible" title="商机赢单 · 生成销售订单" width="760px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 14px"
        title="赢单后将自动创建销售订单草稿，可在「进销存 → 销售订单」中提交审批并出库。" />
      <div class="muted" style="margin-bottom: 10px">商机：{{ current?.name }} · 客户：{{ current?.customerName }}</div>
      <el-table :data="winItems" size="small">
        <el-table-column label="商品" min-width="230">
          <template #default="{ row }">
            <el-select v-model="row.materialId" filterable style="width: 100%" @change="onMaterialChange(row)">
              <el-option v-for="m in materials" :key="m.id" :label="`${m.name}（${m.spec || '-'}）`" :value="m.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="130">
          <template #default="{ row }"><el-input-number v-model="row.quantity" :min="1" size="small" style="width: 100%" /></template>
        </el-table-column>
        <el-table-column label="单价" width="140">
          <template #default="{ row }"><el-input-number v-model="row.price" :min="0" :step="100" size="small" style="width: 100%" /></template>
        </el-table-column>
        <el-table-column label="小计" width="120" align="right">
          <template #default="{ row }">{{ money((row.price || 0) * (row.quantity || 0)) }}</template>
        </el-table-column>
        <el-table-column width="60" align="center">
          <template #default="{ $index }"><el-button link type="danger" @click="winItems.splice($index, 1)">删除</el-button></template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 10px; display: flex; justify-content: space-between; align-items: center">
        <el-button :icon="Plus" size="small" @click="addItem">添加商品</el-button>
        <span class="amount-text">合计：{{ money(winTotal) }}</span>
      </div>
      <template #footer>
        <el-button @click="winVisible = false">取消</el-button>
        <el-button type="success" :loading="saving" @click="doWin">确认赢单并生成订单</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="商机详情" size="520px">
      <div v-if="current">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="商机名称" :span="2">{{ current.name }}</el-descriptions-item>
          <el-descriptions-item label="商机编号">{{ current.oppNo }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ current.customerName }}</el-descriptions-item>
          <el-descriptions-item label="阶段">{{ current.stage }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ current.status }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ money(current.amount) }}</el-descriptions-item>
          <el-descriptions-item label="赢率">{{ current.probability }}%</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ current.ownerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预计成交">{{ d(current.expectedCloseDate) }}</el-descriptions-item>
          <el-descriptions-item label="关联订单">{{ current.orderCount || 0 }} 张</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ current.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="section-title" style="margin-top: 18px">跟进记录</div>
        <el-timeline>
          <el-timeline-item v-for="a in activities" :key="a.id" :timestamp="dt(a.activityTime)">
            <b>{{ a.content }}</b>
            <div class="muted" style="font-size: 12px">{{ a.ownerName || '-' }} · {{ a.type }}</div>
          </el-timeline-item>
          <div v-if="activities.length === 0" class="empty-box">暂无跟进记录</div>
        </el-timeline>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="current?.status === 'OPEN'" type="success" @click="openWin(current)">赢单</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusTag from '../../components/StatusTag.vue'
import { crmApi, erpApi, masterApi, systemApi } from '../../api'
import { d, dt, money } from '../../utils/format'

const stages = [
  { value: 'INITIAL', label: '初步接洽' }, { value: 'NEEDS', label: '需求确认' }, { value: 'SOLUTION', label: '方案报价' },
  { value: 'QUOTE', label: '商务谈判' }, { value: 'WON', label: '赢单' }, { value: 'LOST', label: '输单' }
]

const rows = ref<any[]>([])
const pipeline = ref<any[]>([])
const stats = ref<any>({})
const customers = ref<any[]>([])
const materials = ref<any[]>([])
const users = ref<any[]>([])
const activities = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const stageFilter = ref('')
const formVisible = ref(false)
const winVisible = ref(false)
const detailVisible = ref(false)
const current = ref<any>(null)
const winItems = ref<any[]>([])
const form = reactive<any>({ id: null, name: '', customerId: null, stage: 'INITIAL', amount: 0, probability: 20, expectedCloseDate: '', ownerId: null, remark: '' })

const filtered = computed(() =>
  rows.value.filter((r) => {
    const kw = keyword.value.trim()
    const okKw = !kw || [r.name, r.customerName, r.oppNo].some((v) => String(v || '').includes(kw))
    const okStage = !stageFilter.value || r.stage === stageFilter.value
    return okKw && okStage
  })
)
const winTotal = computed(() => winItems.value.reduce((s, i) => s + (Number(i.price) || 0) * (Number(i.quantity) || 0), 0))

async function load() {
  loading.value = true
  try {
    const [list, pipe, st] = await Promise.all([crmApi.opportunities(), crmApi.pipeline(), crmApi.stats()])
    rows.value = list
    pipeline.value = pipe
    stats.value = st
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, name: '', customerId: customers.value[0]?.id, stage: 'INITIAL', amount: 0, probability: 20, expectedCloseDate: '', ownerId: users.value[0]?.id, remark: '' })
  formVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, row)
  formVisible.value = true
}

async function save() {
  if (!form.name) {
    ElMessage.warning('请输入商机名称')
    return
  }
  saving.value = true
  try {
    if (form.id) await crmApi.updateOpportunity(form.id, form)
    else await crmApi.createOpportunity(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openDetail(row: any) {
  current.value = row
  detailVisible.value = true
  activities.value = await crmApi.activitiesByBiz('OPPORTUNITY', row.id)
}

function openWin(row: any) {
  current.value = row
  winItems.value = [{ materialId: materials.value[0]?.id, quantity: 1, price: materials.value[0]?.salePrice || 0 }]
  winVisible.value = true
}

function onMaterialChange(row: any) {
  const m = materials.value.find((x) => x.id === row.materialId)
  if (m) row.price = Number(m.salePrice || 0)
}

function addItem() {
  winItems.value.push({ materialId: materials.value[0]?.id, quantity: 1, price: materials.value[0]?.salePrice || 0 })
}

async function doWin() {
  if (winItems.value.length === 0 || winItems.value.some((i) => !i.materialId || !i.quantity)) {
    ElMessage.warning('请完善商品明细')
    return
  }
  saving.value = true
  try {
    const order: any = await crmApi.win(current.value.id, winItems.value)
    ElMessage.success(`赢单成功，已生成销售订单 ${order.orderNo}，请前往销售订单提交审批`)
    winVisible.value = false
    detailVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该商机吗？', '提示', { type: 'warning' })
  await crmApi.deleteOpportunity(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  const [c, m, u] = await Promise.all([masterApi.customerOptions(), masterApi.materialOptions(), systemApi.userOptions()])
  customers.value = c
  materials.value = m
  users.value = u
  load()
})
</script>
