<template>
  <div>
    <PageHeader title="库存查询" subtitle="实时库存分布，支持按仓库筛选与盘点调整">
      <template #actions>
        <el-button type="warning" :icon="Tools" @click="openAdjust">盘点调整</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid" style="grid-template-columns: repeat(3, 1fr); margin-bottom: 14px">
      <div class="kpi-card">
        <div class="kpi-label">库存总金额</div>
        <div class="kpi-value">{{ money(summary.value) }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">库存总数量</div>
        <div class="kpi-value">{{ summary.quantity ?? 0 }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">预警物料数</div>
        <div class="kpi-value" style="color: #e6a23c">{{ summary.alertCount ?? 0 }}</div>
      </div>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-select v-model="query.warehouseId" placeholder="全部仓库" clearable style="width: 180px" @change="load">
          <el-option v-for="w in warehouses" :key="w.id" :value="w.id" :label="w.name" />
        </el-select>
        <el-checkbox v-model="query.alertOnly" @change="load">仅看预警</el-checkbox>
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ rows.length }} 条库存记录</el-tag>
      </div>

      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="materialCode" label="物料编号" width="105" />
        <el-table-column prop="materialName" label="物料名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" width="105" />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="quantity" label="现存数量" width="100" align="right" />
        <el-table-column prop="lockedQuantity" label="锁定数量" width="90" align="right" />
        <el-table-column prop="unit" label="单位" width="60" align="center" />
        <el-table-column prop="safeStock" label="安全库存" width="90" align="right" />
        <el-table-column label="库存金额" width="115" align="right">
          <template #default="{ row }">{{ money((row.quantity || 0) * (row.costPrice || 0)) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.alert" type="danger">预警</el-tag>
            <el-tag v-else type="success">正常</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="adjustVisible" title="盘点调整" width="480px">
      <el-alert type="info" :closable="false" title="正数为入库调整，负数为出库调整，将生成 ADJUST 类型台账" style="margin-bottom: 14px" />
      <el-form :model="adjustForm" label-width="90px">
        <el-form-item label="物料">
          <el-select v-model="adjustForm.materialId" filterable style="width: 100%" placeholder="选择物料">
            <el-option v-for="m in materials" :key="m.id" :value="m.id" :label="m.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="adjustForm.warehouseId" style="width: 100%" placeholder="选择仓库">
            <el-option v-for="w in warehouses" :key="w.id" :value="w.id" :label="w.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="调整数量">
          <el-input-number v-model="adjustForm.quantity" style="width: 100%" placeholder="正数入库 / 负数出库" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Tools } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { erpApi, masterApi } from '../../api'
import { money } from '../../utils/format'

const rows = ref<any[]>([])
const summary = ref<any>({})
const loading = ref(false)
const saving = ref(false)
const adjustVisible = ref(false)
const warehouses = ref<any[]>([])
const materials = ref<any[]>([])
const query = reactive<{ warehouseId: number | null; alertOnly: boolean }>({ warehouseId: null, alertOnly: false })
const adjustForm = reactive<any>({ materialId: null, warehouseId: null, quantity: 0 })

async function load() {
  loading.value = true
  try {
    const params: any = { alertOnly: query.alertOnly }
    if (query.warehouseId) params.warehouseId = query.warehouseId
    rows.value = await erpApi.stock(params)
    summary.value = await erpApi.stockValue()
  } finally {
    loading.value = false
  }
}

async function openAdjust() {
  if (!warehouses.value.length) warehouses.value = await masterApi.warehouses()
  if (!materials.value.length) materials.value = await masterApi.materialOptions()
  Object.assign(adjustForm, { materialId: null, warehouseId: null, quantity: 0 })
  adjustVisible.value = true
}

async function doAdjust() {
  if (!adjustForm.materialId || !adjustForm.warehouseId || !adjustForm.quantity) {
    return ElMessage.warning('请选择物料、仓库并填写非零调整数量')
  }
  saving.value = true
  try {
    await erpApi.adjust(adjustForm)
    ElMessage.success('调整成功')
    adjustVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  warehouses.value = await masterApi.warehouses()
  load()
})
</script>
