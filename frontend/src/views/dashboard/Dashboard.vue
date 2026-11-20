<template>
  <div>
    <PageHeader title="经营驾驶舱" subtitle="贯通 OA · SRM · ERP · CRM 的一体化经营视图">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新数据</el-button>
        <el-button type="primary" :icon="Plus" @click="router.push('/crm/leads')">新建线索</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <div class="kpi-card" v-for="item in kpiCards" :key="item.label">
        <div class="kpi-label">{{ item.label }}</div>
        <div class="kpi-value">{{ item.value }}<small v-if="item.unit">{{ item.unit }}</small></div>
        <div class="kpi-foot">{{ item.foot }}</div>
        <el-icon class="kpi-icon"><component :is="item.icon" /></el-icon>
      </div>
    </div>

    <div class="grid-2">
      <div class="page-card">
        <div class="section-title">销售与采购趋势（近 6 个月）</div>
        <div ref="trendRef" class="chart-box"></div>
      </div>
      <div class="page-card">
        <div class="section-title">CRM 商机漏斗</div>
        <div ref="funnelRef" class="chart-box"></div>
      </div>
    </div>

    <div class="grid-2" style="margin-top: 16px">
      <div class="page-card">
        <div class="section-title">我的待办</div>
        <div v-if="todoItems.length === 0" class="empty-box">暂无待办事项</div>
        <div v-for="item in todoItems" :key="item.key" class="todo-item">
          <div class="todo-dot" :style="{ background: item.color }"></div>
          <div class="todo-main">
            <b>{{ item.title }}</b>
            <span>{{ item.sub }}</span>
          </div>
          <el-button v-if="item.type === 'APPROVAL'" link type="primary" @click="router.push('/workflow/todo')">去审批</el-button>
          <el-button v-else link type="primary" @click="router.push('/oa/tasks')">去处理</el-button>
        </div>
        <div style="margin-top: 10px; display: flex; gap: 8px">
          <el-tag type="warning" effect="plain">待审批 {{ todos.approvals?.length || 0 }}</el-tag>
          <el-tag type="primary" effect="plain">待办任务 {{ todos.tasks?.length || 0 }}</el-tag>
          <el-tag type="danger" effect="plain">待跟进线索 {{ todos.leadFollow || 0 }}</el-tag>
        </div>
      </div>

      <div class="page-card">
        <div class="section-title">最新公告</div>
        <div v-if="overview.notices?.length === 0" class="empty-box">暂无公告</div>
        <div v-for="n in overview.notices" :key="n.id" class="todo-item">
          <el-tag size="small" type="info" effect="plain">{{ n.category }}</el-tag>
          <div class="todo-main">
            <b>{{ n.title }}</b>
            <span>{{ dt(n.publishTime) }}</span>
          </div>
        </div>
        <el-divider style="margin: 14px 0" />
        <div class="section-title">最近跟进动态</div>
        <div v-if="overview.activities?.length === 0" class="empty-box">暂无跟进记录</div>
        <div v-for="a in overview.activities" :key="a.id" class="todo-item">
          <el-tag size="small" effect="plain">{{ activityType(a.type) }}</el-tag>
          <div class="todo-main">
            <b>{{ a.content }}</b>
            <span>{{ a.ownerName || '-' }} · {{ dt(a.activityTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="grid-2-equal" style="margin-top: 16px">
      <div class="page-card">
        <div class="section-title">最近待审批单据</div>
        <el-table :data="overview.recentApprovals || []" size="small">
          <el-table-column prop="title" label="审批事项" min-width="200" show-overflow-tooltip />
          <el-table-column prop="module" label="模块" width="72" align="center" />
          <el-table-column prop="applicantName" label="申请人" width="88" />
          <el-table-column prop="currentNodeName" label="当前节点" width="110" />
          <el-table-column label="金额" width="100" align="right">
            <template #default="{ row }">{{ money(row.amount) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-card">
        <div class="section-title">库存预警与重点客户</div>
        <el-table :data="overview.stockAlerts || []" size="small">
          <el-table-column prop="materialName" label="预警物料" min-width="150" show-overflow-tooltip />
          <el-table-column prop="warehouseName" label="仓库" width="90" />
          <el-table-column prop="quantity" label="现存量" width="80" align="right" />
          <el-table-column prop="safeStock" label="安全库存" width="90" align="right" />
        </el-table>
        <el-divider style="margin: 12px 0" />
        <div class="section-title">销售额 TOP 客户</div>
        <div v-for="(c, idx) in overview.topCustomers || []" :key="idx" class="todo-item">
          <el-tag size="small" :type="idx === 0 ? 'danger' : 'info'" effect="plain">{{ idx + 1 }}</el-tag>
          <div class="todo-main"><b>{{ c.customerName }}</b></div>
          <span class="amount-text">{{ money(c.amount) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Refresh, Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { dashboardApi } from '../../api'
import { dt, money } from '../../utils/format'

const router = useRouter()
const overview = ref<any>({ kpi: {}, salesTrend: [], purchaseTrend: [], funnel: [], recentApprovals: [], notices: [], activities: [], stockAlerts: [], topCustomers: [] })
const todos = ref<any>({ approvals: [], tasks: [], leadFollow: 0 })
const trendRef = ref<HTMLDivElement>()
const funnelRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let funnelChart: echarts.ECharts | null = null

const kpiCards = computed(() => {
  const k = overview.value.kpi || {}
  return [
    { label: '本月销售额', value: money(k.monthSales), unit: '', foot: `本月订单 ${k.monthOrders || 0} 张`, icon: 'Money' },
    { label: 'CRM 商机金额', value: money(k.crmPipeline), unit: '', foot: `赢单率 ${k.winRate || 0}% · 新线索 ${k.newLeads || 0} 条`, icon: 'TrendCharts' },
    { label: '客户 / 供应商', value: `${k.customers || 0} / ${k.suppliers || 0}`, unit: '', foot: `在册员工 ${k.employees || 0} 人`, icon: 'UserFilled' },
    { label: '库存总金额', value: money(k.inventoryValue), unit: '', foot: `库存预警 ${k.stockAlerts || 0} 项`, icon: 'Box' },
    { label: '待我审批', value: k.pendingApprovals || 0, unit: '项', foot: '统一审批中心汇总', icon: 'Checked' },
    { label: '采购执行中', value: k.purchasePending || 0, unit: '单', foot: `待审批需求 ${k.requisitionPending || 0} 单`, icon: 'ShoppingCart' },
    { label: '本月新增客户', value: k.customers || 0, unit: '家', foot: '含线索转化客户', icon: 'OfficeBuilding' },
    { label: '待处理事项', value: todos.value.total || 0, unit: '项', foot: '审批 + 任务 + 线索跟进', icon: 'Bell' }
  ]
})

const todoItems = computed(() => {
  const list: any[] = []
  ;(todos.value.approvals || []).slice(0, 5).forEach((a: any) => {
    list.push({ key: 'a' + a.approvalId, type: 'APPROVAL', color: '#e6a23c', title: a.title, sub: `${a.applicantName || ''} · ${a.module} · ${money(a.amount)}` })
  })
  ;(todos.value.tasks || []).slice(0, 4).forEach((t: any) => {
    list.push({ key: 't' + t.id, type: 'TASK', color: t.priority === 'HIGH' ? '#f56c6c' : '#409eff', title: t.title, sub: `截止 ${t.dueDate || '-'} · ${t.priority}` })
  })
  return list
})

function activityType(type: string) {
  const map: Record<string, string> = { CALL: '电话', VISIT: '拜访', EMAIL: '邮件', MEETING: '会议', OTHER: '其他' }
  return map[type] || type
}

function renderCharts() {
  const sales = overview.value.salesTrend || []
  const purchase = overview.value.purchaseTrend || []
  const months = sales.map((s: any) => s.month)
  if (trendRef.value) {
    trendChart = trendChart || echarts.init(trendRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['销售额', '订单数', '采购额'], right: 0, top: 0, textStyle: { fontSize: 11 } },
      grid: { left: 10, right: 16, top: 38, bottom: 6, containLabel: true },
      xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: '#dce5ef' } }, axisLabel: { color: '#7b8da1' } },
      yAxis: [
        { type: 'value', name: '金额', axisLabel: { color: '#7b8da1', formatter: (v: number) => (v >= 10000 ? (v / 10000).toFixed(0) + '万' : v) }, splitLine: { lineStyle: { color: '#eef2f7' } } },
        { type: 'value', name: '单数', axisLabel: { color: '#7b8da1' }, splitLine: { lineStyle: { color: '#eef2f7' } } }
      ],
      series: [
        { name: '销售额', type: 'bar', barWidth: 18, itemStyle: { color: '#2f6bb3', borderRadius: [4, 4, 0, 0] }, data: sales.map((s: any) => s.amount) },
        { name: '采购额', type: 'line', smooth: true, symbolSize: 6, itemStyle: { color: '#5aa87a' }, areaStyle: { color: 'rgba(90,168,122,.08)' }, data: purchase.map((p: any) => p.amount) },
        { name: '订单数', type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 6, itemStyle: { color: '#e6a23c' }, data: sales.map((s: any) => s.orders) }
      ]
    })
  }
  if (funnelRef.value) {
    funnelChart = funnelChart || echarts.init(funnelRef.value)
    const funnel = overview.value.funnel || []
    funnelChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 10, right: 26, top: 20, bottom: 6, containLabel: true },
      xAxis: { type: 'value', axisLabel: { color: '#7b8da1' }, splitLine: { lineStyle: { color: '#eef2f7' } } },
      yAxis: { type: 'category', inverse: true, data: funnel.map((f: any) => f.stageName), axisLabel: { color: '#52647a' } },
      series: [{
        type: 'bar', barWidth: 16,
        label: { show: true, position: 'right', color: '#52647a', fontSize: 11, formatter: (p: any) => `${funnel[p.dataIndex]?.count || 0} 个` },
        itemStyle: {
          color: (p: any) => ['#9db9d6', '#7fa6cc', '#5f8fc0', '#3d75b0', '#2f8f5b', '#c98b8b'][p.dataIndex] || '#2f6bb3',
          borderRadius: [0, 4, 4, 0]
        },
        data: funnel.map((f: any) => Number(f.amount || 0))
      }]
    })
  }
}

async function load() {
  try {
    const [ov, td] = await Promise.all([dashboardApi.overview(), dashboardApi.todos()])
    overview.value = ov
    todos.value = td
    await nextTick()
    renderCharts()
  } catch {
    /* handled */
  }
}

function handleResize() {
  trendChart?.resize()
  funnelChart?.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  funnelChart?.dispose()
})
</script>
