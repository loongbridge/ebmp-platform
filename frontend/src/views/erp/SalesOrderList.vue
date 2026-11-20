<template>
  <div>
    <PageHeader title="销售订单" subtitle="CRM 赢单或直接下单，审批通过后出库并扣减库存">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建销售订单</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="DRAFT">草稿</el-radio-button>
          <el-radio-button value="PENDING">审批中</el-radio-button>
          <el-radio-button value="APPROVED">待出库</el-radio-button>
          <el-radio-button value="OUTBOUND">已出库</el-radio-button>
          <el-radio-button value="DONE">已完成</el-radio-button>
        </el-radio-group>
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ filtered.length }} 条</el-tag>
      </div>

      <el-table :data="filtered" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="150" />
        <el-table-column prop="customerName" label="客户" min-width="170" show-overflow-tooltip />
        <el-table-column prop="itemCount" label="明细" width="65" align="center" />
        <el-table-column label="订单金额" width="115" align="right">
          <template #default="{ row }">{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="出库进度" width="130" align="center">
          <template #default="{ row }"><el-progress :percentage="Number(row.shippedPercent || 0)" :stroke-width="8" /></template>
        </el-table-column>
        <el-table-column prop="orderDate" label="下单日期" width="100" />
        <el-table-column prop="creatorName" label="销售员" width="85" />
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="185" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">明细</el-button>
            <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="warning" @click="submit(row)">提交审批</el-button>
            <el-button v-if="row.status === 'APPROVED'" link type="success" @click="openOutbound(row)">出库发货</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" title="新建销售订单" width="780px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="14">
            <el-form-item label="客户">
              <el-select v-model="form.customerId" filterable style="width: 100%" placeholder="选择客户">
                <el-option v-for="c in customers" :key="c.id" :value="c.id" :label="c.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="10"><el-form-item label="下单日期"><el-date-picker v-model="form.orderDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="销售明细">
          <el-table :data="form.items" size="small" border>
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <el-select v-model="row.materialId" filterable placeholder="选择商品" style="width: 100%" @change="(v: number) => onMaterial(row, v)">
                  <el-option v-for="m in materials" :key="m.id" :value="m.id" :label="`${m.name}（${m.spec || '-'}）`" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="130">
              <template #default="{ row }"><el-input-number v-model="row.quantity" :min="1" style="width: 100%" @change="calc" /></template>
            </el-table-column>
            <el-table-column label="单价" width="140">
              <template #default="{ row }"><el-input-number v-model="row.price" :min="0" style="width: 100%" @change="calc" /></template>
            </el-table-column>
            <el-table-column label="小计" width="110" align="right">
              <template #default="{ row }">{{ money((row.quantity || 0) * (row.price || 0)) }}</template>
            </el-table-column>
            <el-table-column label="" width="60" align="center">
              <template #default="{ $index }"><el-button link type="danger" @click="form.items.splice($index, 1); calc()">删除</el-button></template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 8px; display: flex; justify-content: space-between; align-items: center">
            <el-button size="small" :icon="Plus" @click="form.items.push({ materialId: null, quantity: 1, price: 0 })">添加明细</el-button>
            <span style="font-weight: 600">合计：{{ money(form.totalAmount) }}</span>
          </div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="outboundVisible" title="出库发货" width="420px">
      <el-alert type="warning" :closable="false" title="出库后将按订单明细扣减对应仓库库存，库存不足会失败" style="margin-bottom: 14px" />
      <el-form label-width="90px">
        <el-form-item label="出库仓库">
          <el-select v-model="outboundWarehouseId" style="width: 100%" placeholder="选择仓库">
            <el-option v-for="w in warehouses" :key="w.id" :value="w.id" :label="w.name" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outboundVisible = false">取消</el-button>
        <el-button type="success" :loading="saving" @click="doOutbound">确认出库</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="`订单明细 - ${detail?.orderNo || ''}`" width="660px">
      <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
        <el-descriptions-item label="客户">{{ detail?.customerName }}</el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :status="detail?.status" /></el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ money(detail?.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="下单日期">{{ detail?.orderDate || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.items || []" size="small" border>
        <el-table-column prop="materialName" label="商品" min-width="150" />
        <el-table-column prop="spec" label="规格" width="95" />
        <el-table-column prop="quantity" label="数量" width="75" align="right" />
        <el-table-column prop="shippedQty" label="已出库" width="75" align="right" />
        <el-table-column label="单价" width="95" align="right">
          <template #default="{ row }">{{ money(row.price) }}</template>
        </el-table-column>
        <el-table-column label="小计" width="105" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusTag from '../../components/StatusTag.vue'
import { erpApi, masterApi } from '../../api'
import { money } from '../../utils/format'

const rows = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const statusFilter = ref('')
const formVisible = ref(false)
const outboundVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<any>(null)
const current = ref<any>(null)
const outboundWarehouseId = ref<number | null>(null)
const customers = ref<any[]>([])
const materials = ref<any[]>([])
const warehouses = ref<any[]>([])
const form = reactive<any>({ customerId: null, orderDate: '', remark: '', items: [], totalAmount: 0 })

const filtered = computed(() => (statusFilter.value ? rows.value.filter((r) => r.status === statusFilter.value) : rows.value))

async function load() {
  loading.value = true
  try {
    rows.value = await erpApi.orders()
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  if (!customers.value.length) customers.value = await masterApi.customerOptions()
  if (!materials.value.length) materials.value = await masterApi.materialOptions()
  Object.assign(form, { customerId: null, orderDate: new Date().toISOString().slice(0, 10), remark: '', items: [{ materialId: null, quantity: 1, price: 0 }], totalAmount: 0 })
  formVisible.value = true
}

function onMaterial(item: any, materialId: number) {
  const m = materials.value.find((x) => x.id === materialId)
  if (m) item.price = m.salePrice || 0
  calc()
}

function calc() {
  form.items.forEach((i: any) => (i.amount = (i.quantity || 0) * (i.price || 0)))
  form.totalAmount = form.items.reduce((s: number, i: any) => s + (i.amount || 0), 0)
}

async function save() {
  if (!form.customerId) return ElMessage.warning('请选择客户')
  if (!form.items.length || form.items.some((i: any) => !i.materialId)) return ElMessage.warning('请完善销售明细')
  calc()
  saving.value = true
  try {
    await erpApi.createOrder(form)
    ElMessage.success('已保存草稿，可在列表中提交审批')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function submit(row: any) {
  await ElMessageBox.confirm(`确定提交订单「${row.orderNo}」进入审批流吗？`, '提交审批', { type: 'warning' })
  await erpApi.submitOrder(row.id)
  ElMessage.success('已提交审批')
  load()
}

async function openOutbound(row: any) {
  if (!warehouses.value.length) warehouses.value = await masterApi.warehouses()
  current.value = row
  outboundWarehouseId.value = warehouses.value[0]?.id || null
  outboundVisible.value = true
}

async function doOutbound() {
  if (!outboundWarehouseId.value) return ElMessage.warning('请选择出库仓库')
  saving.value = true
  try {
    await erpApi.outbound(current.value.id, outboundWarehouseId.value)
    ElMessage.success('出库成功，库存已扣减')
    outboundVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openDetail(row: any) {
  detail.value = await erpApi.order(row.id)
  detailVisible.value = true
}

onMounted(load)
</script>
