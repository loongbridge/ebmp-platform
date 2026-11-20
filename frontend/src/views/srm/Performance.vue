<template>
  <div>
    <PageHeader title="供应商绩效" subtitle="按供应商汇总采购订单规模与最新综合评分" />

    <div class="page-card">
      <div class="chart-box" ref="chartRef" style="height: 320px"></div>
    </div>

    <div class="page-card">
      <div class="section-title">绩效明细</div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="supplierName" label="供应商" min-width="180" show-overflow-tooltip />
        <el-table-column prop="orderCount" label="采购订单数" width="110" align="center" />
        <el-table-column label="累计采购金额" width="140" align="right">
          <template #default="{ row }">{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="综合评分" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.score >= 90 ? 'success' : row.score >= 75 ? 'primary' : row.score > 0 ? 'danger' : 'info'" effect="dark">
              {{ row.score || '未评价' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.score > 0" :type="row.score >= 90 ? 'success' : row.score >= 75 ? 'primary' : 'danger'">
              {{ row.score >= 90 ? 'A 级' : row.score >= 75 ? 'B 级' : 'C 级' }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import PageHeader from '../../components/PageHeader.vue'
import { srmApi } from '../../api'
import { money } from '../../utils/format'

const rows = ref<any[]>([])
const loading = ref(false)
const chartRef = ref<HTMLElement>()

async function load() {
  loading.value = true
  try {
    rows.value = await srmApi.performance()
    renderChart()
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value) return
  const chart = echarts.init(chartRef.value)
  const names = rows.value.map((r) => r.supplierName)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['采购金额', '综合评分'] },
    grid: { left: 60, right: 50, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: names },
    yAxis: [
      { type: 'value', name: '金额' },
      { type: 'value', name: '评分', max: 100 }
    ],
    series: [
      { name: '采购金额', type: 'bar', data: rows.value.map((r) => r.totalAmount), itemStyle: { color: '#1f4e8c', borderRadius: [4, 4, 0, 0] } },
      { name: '综合评分', type: 'line', yAxisIndex: 1, data: rows.value.map((r) => r.score), itemStyle: { color: '#e6a23c' }, smooth: true }
    ]
  })
  window.addEventListener('resize', () => chart.resize())
}

onMounted(load)
</script>
