<template>
  <div>
    <PageHeader title="请假管理" subtitle="请假申请统一进入平台审批流，审批结果自动回写">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">发起请假</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-switch v-model="mine" active-text="仅看我的请假" @change="load" />
        <div class="spacer"></div>
        <el-tag type="warning" effect="plain">待审批 {{ pendingCount }} 单</el-tag>
      </div>
      <el-table :data="rows" v-loading="loading" size="default" empty-text="暂无请假记录">
        <el-table-column prop="leaveNo" label="请假单号" width="160" />
        <el-table-column prop="userName" label="申请人" width="90" />
        <el-table-column prop="deptName" label="部门" width="110" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }"><StatusTag :status="row.type" /></template>
        </el-table-column>
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ dt(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="160">
          <template #default="{ row }">{{ dt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="days" label="天数" width="70" align="center" />
        <el-table-column prop="reason" label="事由" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="['DRAFT', 'REJECTED'].includes(row.status)" link type="primary" @click="submit(row)">提交审批</el-button>
            <span v-else class="muted" style="font-size: 12px">已提交</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" title="发起请假" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="请假类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="年假" value="ANNUAL" />
            <el-option label="病假" value="SICK" />
            <el-option label="事假" value="PERSONAL" />
            <el-option label="婚假" value="MARRIAGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="请假天数">
          <el-input-number v-model="form.days" :min="0.5" :step="0.5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="请假事由">
          <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请填写请假原因及工作交接安排" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusTag from '../../components/StatusTag.vue'
import { oaApi } from '../../api'
import { dt } from '../../utils/format'

const rows = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const mine = ref(false)
const formVisible = ref(false)
const form = reactive<any>({ type: 'ANNUAL', startTime: '', endTime: '', days: 1, reason: '' })
const pendingCount = computed(() => rows.value.filter((r) => r.status === 'PENDING').length)

async function load() {
  loading.value = true
  try {
    rows.value = await oaApi.leaves({ mine: mine.value })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { type: 'ANNUAL', startTime: '', endTime: '', days: 1, reason: '' })
  formVisible.value = true
}

async function save() {
  if (!form.startTime || !form.endTime) {
    ElMessage.warning('请选择请假起止时间')
    return
  }
  saving.value = true
  try {
    await oaApi.createLeave(form)
    ElMessage.success('请假单已保存为草稿')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function submit(row: any) {
  await ElMessageBox.confirm('提交后将进入审批流，确定提交吗？', '提交审批', { type: 'warning' })
  await oaApi.submitLeave(row.id)
  ElMessage.success('已提交审批')
  load()
}

onMounted(load)
</script>
