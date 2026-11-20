<template>
  <div>
    <PageHeader title="考勤打卡" subtitle="每日上下班打卡与团队出勤概览">
      <template #actions>
        <el-button type="success" :icon="Clock" :loading="acting" @click="checkIn">上班打卡</el-button>
        <el-button type="warning" :icon="Clock" :loading="acting" @click="checkOut">下班打卡</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">今日已打卡</div>
        <div class="kpi-value">{{ summary.checkedIn || 0 }}<small>/ {{ summary.totalUsers || 0 }} 人</small></div>
        <div class="kpi-foot">应出勤员工总数</div>
        <el-icon class="kpi-icon"><UserFilled /></el-icon>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">正常出勤</div>
        <div class="kpi-value" style="color: #2f8f5b">{{ summary.normal || 0 }}<small>人</small></div>
        <div class="kpi-foot">状态正常</div>
        <el-icon class="kpi-icon"><CircleCheck /></el-icon>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">迟到</div>
        <div class="kpi-value" style="color: #c98b2e">{{ summary.late || 0 }}<small>人</small></div>
        <div class="kpi-foot">09:05 后打卡</div>
        <el-icon class="kpi-icon"><Warning /></el-icon>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">统计日期</div>
        <div class="kpi-value" style="font-size: 19px">{{ summary.date || today }}</div>
        <div class="kpi-foot">实时出勤汇总</div>
        <el-icon class="kpi-icon"><Calendar /></el-icon>
      </div>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-switch v-model="mine" active-text="仅看我的考勤" @change="load" />
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="按日期筛选" clearable style="width: 170px" @change="load" />
        <div class="spacer"></div>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" size="default" empty-text="暂无考勤记录">
        <el-table-column prop="userName" label="员工" width="110" />
        <el-table-column label="日期" width="120">
          <template #default="{ row }">{{ d(row.workDate) }}</template>
        </el-table-column>
        <el-table-column label="上班打卡" width="170">
          <template #default="{ row }">{{ dt(row.checkInTime) }}</template>
        </el-table-column>
        <el-table-column label="下班打卡" width="170">
          <template #default="{ row }">{{ dt(row.checkOutTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Refresh } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusTag from '../../components/StatusTag.vue'
import { oaApi } from '../../api'
import { d, dt } from '../../utils/format'

const rows = ref<any[]>([])
const loading = ref(false)
const acting = ref(false)
const mine = ref(false)
const date = ref('')
const summary = ref<any>({})
const today = computed(() => new Date().toISOString().slice(0, 10))

async function load() {
  loading.value = true
  try {
    rows.value = await oaApi.attendance({ mine: mine.value, date: date.value || undefined })
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  summary.value = await oaApi.attendanceSummary()
}

async function checkIn() {
  acting.value = true
  try {
    await oaApi.checkIn()
    ElMessage.success('上班打卡成功')
    load()
    loadSummary()
  } finally {
    acting.value = false
  }
}

async function checkOut() {
  acting.value = true
  try {
    await oaApi.checkOut()
    ElMessage.success('下班打卡成功')
    load()
  } finally {
    acting.value = false
  }
}

onMounted(() => {
  load()
  loadSummary()
})
</script>
