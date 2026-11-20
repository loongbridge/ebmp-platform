<template>
  <div>
    <PageHeader title="库存预警" subtitle="现存数量低于安全库存的物料清单，建议及时发起采购需求" />

    <div class="page-card">
      <el-alert v-if="rows.length" type="warning" :closable="false" :title="`当前有 ${rows.length} 条库存低于安全水位，请及时补货`" style="margin-bottom: 14px" />
      <el-empty v-if="!loading && !rows.length" description="库存水位健康，暂无预警" />
      <el-table v-else :data="rows" v-loading="loading">
        <el-table-column prop="materialCode" label="物料编号" width="105" />
        <el-table-column prop="materialName" label="物料名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" width="105" />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="quantity" label="现存数量" width="100" align="right">
          <template #default="{ row }"><span style="color: #f56c6c; font-weight: 600">{{ row.quantity }}</span></template>
        </el-table-column>
        <el-table-column prop="safeStock" label="安全库存" width="90" align="right" />
        <el-table-column prop="unit" label="单位" width="60" align="center" />
        <el-table-column label="缺口" width="90" align="right">
          <template #default="{ row }">{{ (row.safeStock || 0) - (row.quantity || 0) }}</template>
        </el-table-column>
        <el-table-column label="建议" min-width="140">
          <template #default><el-tag type="warning" effect="plain">建议发起采购补货</el-tag></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '../../components/PageHeader.vue'
import { erpApi } from '../../api'

const rows = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = await erpApi.alerts()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
