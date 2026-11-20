<template>
  <div>
    <PageHeader title="采购订单" subtitle="向供应商下单，审批通过后可收货入库并联动库存">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建采购订单</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="applyFilter">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="DRAFT">草稿</el-radio-button>
          <el-radio-button value="PENDING">审批中</el-radio-button>
          <el-radio-button value="APPROVED">待收货</el-radio-button>
          <el-radio-button value="RECEIVED">已入库</el-radio-button>
        </el-radio-group>
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ filtered.length }} 条</el-tag>
      </div>

      <el-table :data="filtered" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="150" />
        <el-table-column prop="supplierName" label="供应商" min-width="170" show-overflow-tooltip />
        <el-table-column prop="itemCount" label="明细" width="65" align="center" />
        <el-table-column label="订单金额" width="115" align="right">
          <template #default="{ row }">{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="收货进度" width="130" align="center">
          <template #default="{ row }"><el-progress :percentage="row.receivedPercent || 0" :stroke-width="8" /></template>
        </el-table-column>
        <el-table-column prop="expectedDate" label="预计到货" width="100" />
        <el-table-column prop="creatorName" label="采购员" width="85" />
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="185" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">明细</el-button>
            <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="warning" @click="submit(row)">提交审批</el-button>
            <el-button v-if="row.status === 'APPROVED' || row.status === 'PARTIALLY_RECEIVED'" link type="success" @click="openReceive(row)">收货入库</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" title="新建采购订单" width="780px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="14">
            <el-form-item label="供应商">
              <el-select v-model="form.supplierId" filterable style="width: 100%" placeholder="选择供应商">
                <el-option v-for="s in suppliers" :key="s.id" :value="s.id" :label="s.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="10"><el-form-item label="预计到货"><el-date-picker v-model="form.expectedDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="采购明细">
          <el-table :data="form.items" size="small" border>
            <el-table-column label="物料" min-width="200">
              <template #default="{ row }">
                <el-select v-model="row.materialId" filterable placeholder="选择物料" style="width: 100%" @change="(v: number) => onMaterial(row, v)">
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

    <el-dialog v-model="receiveVisible" title="收货入库" width="420px">
      <el-alert type="info" :closable="false" title="确认收货后将按订单明细增加对应仓库库存" style="margin-bottom: 14px" />
      <el-form label-width="90px">
        <el-form-item label="入库仓库">
          <el-select v-model="receiveWarehouseId" style="width: 100%" placeholder="选择仓库">
            <el-option v-for="w in warehouses" :key="w.id" :value="w.id" :label="w.name" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="receiveVisible = false">取消</el-button>
        <el-button type="success" :loading="saving" @click="doReceive">确认收货</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="`订单明细 - ${detail?.orderNo || ''}`" width="660px">
      <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
        <el-descriptions-item label="供应商">{{ detail?.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :status="detail?.status" /></el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ money(detail?.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="预计到货">{{ detail?.expectedDate || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.items || []" size="small" border>
        <el-table-column prop="materialName" label="物料" min-width="150" />
        <el-table-column prop="spec" label="规格" width="95" />
        <el-table-column prop="quantity" label="数量" width="75" align="right" />
        <el-table-column prop="receivedQty" label="已收货" width="75" align="right" />
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
import { masterApi, srmApi } from '../../api'
import { money } from '../../utils/format'

const rows = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const statusFilter = ref('')
const formVisible = ref(false)
const receiveVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<any>(null)
const current = ref<any>(null)
const receiveWarehouseId = ref<number | null>(null)
const suppliers = ref<any[]>([])
const materials = ref<any[]>([])
const warehouses = ref<any[]>([])
const form = reactive<any>({ supplierId: null, expectedDate: '', remark: '', items: [], totalAmount: 0 })

const filtered = computed(() => (statusFilter.value ? rows.value.filter((r) => r.status === statusFilter.value) : rows.value))

function applyFilter() {}

async function load() {
  loading.value = true
  try {
    rows.value = await srmApi.orders()
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  if (!suppliers.value.length) suppliers.value = await masterApi.supplierOptions()
  if (!materials.value.length) materials.value = await masterApi.materialOptions()
  Object.assign(form, { supplierId: null, expectedDate: '', remark: '', items: [{ materialId: null, quantity: 1, price: 0 }], totalAmount: 0 })
  formVisible.value = true
}

function onMaterial(item: any, materialId: number) {
  const m = materials.value.find((x) => x.id === materialId)
  if (m) item.price = m.costPrice || 0
  calc()
}

function calc() {
  form.items.forEach((i: any) => (i.amount = (i.quantity || 0) * (i.price || 0)))
  form.totalAmount = form.items.reduce((s: number, i: any) => s + (i.amount || 0), 0)
}

async function save() {
  if (!form.supplierId) return ElMessage.warning('请选择供应商')
  if (!form.items.length || form.items.some((i: any) => !i.materialId)) return ElMessage.warning('请完善采购明细')
  calc()
  saving.value = true
  try {
    await srmApi.createOrder(form)
    ElMessage.success('已保存草稿，可在列表中提交审批')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function submit(row: any) {
  await ElMessageBox.confirm(`确定提交订单「${row.orderNo}」进入审批流吗？`, '提交审批', { type: 'warning' })
  await srmApi.submitOrder(row.id)
  ElMessage.success('已提交审批')
  load()
}

async function openReceive(row: any) {
  if (!warehouses.value.length) warehouses.value = await masterApi.warehouses()
  current.value = row
  receiveWarehouseId.value = warehouses.value[0]?.id || null
  receiveVisible.value = true
}

async function doReceive() {
  if (!receiveWarehouseId.value) return ElMessage.warning('请选择入库仓库')
  saving.value = true
  try {
    await srmApi.receive(current.value.id, receiveWarehouseId.value)
    ElMessage.success('收货成功，库存已更新')
    receiveVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openDetail(row: any) {
  detail.value = await srmApi.order(row.id)
  detailVisible.value = true
}

onMounted(load)
</script>
