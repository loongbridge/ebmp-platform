<template>
  <div>
    <PageHeader title="出入库台账" subtitle="最近 100 条库存流水，含采购入库、销售出库与盘点调整" />

    <div class="page-card">
      <div class="toolbar">
        <el-radio-group v-model="typeFilter">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="IN">入库</el-radio-button>
          <el-radio-button value="OUT">出库</el-radio-button>
        </el-radio-group>
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ filtered.length }} 条流水</el-tag>
      </div>

      <el-table :data="filtered" v-loading="loading">
        <el-table-column prop="txnNo" label="流水号" width="160" />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'IN' ? 'success' : 'danger'">{{ row.type === 'IN' ? '入库' : '出库' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialCode" label="物料编号" width="100" />
        <el-table-column prop="materialName" label="物料名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column prop="unit" label="单位" width="60" align="center" />
        <el-table-column prop="beforeQuantity" label="变动前" width="85" align="right" />
        <el-table-column prop="afterQuantity" label="变动后" width="85" align="right" />
        <el-table-column label="来源" width="110" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" :type="bizTypeMap[row.bizType]?.type || 'info'">{{ bizTypeMap[row.bizType]?.label || row.bizType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bizNo" label="来源单号" width="140" show-overflow-tooltip />
        <el-table-column prop="operatorName" label="操作人" width="85" />
        <el-table-column label="时间" width="155">
          <template #default="{ row }">{{ dt(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '../../components/PageHeader.vue'
import { erpApi } from '../../api'
import { dt } from '../../utils/format'

const rows = ref<any[]>([])
const loading = ref(false)
const typeFilter = ref('')

const bizTypeMap: Record<string, { label: string; type: string }> = {
  PURCHASE: { label: '采购入库', type: 'success' },
  SALES: { label: '销售出库', type: 'danger' },
  ADJUST: { label: '盘点调整', type: 'warning' }
}

const filtered = computed(() => (typeFilter.value ? rows.value.filter((r) => r.type === typeFilter.value) : rows.value))

async function load() {
  loading.value = true
  try {
    rows.value = await erpApi.ledger()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
