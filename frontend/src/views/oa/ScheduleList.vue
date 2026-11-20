<template>
  <div>
    <PageHeader title="日程安排" subtitle="个人与团队日程统一管理，支持提醒与地点">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建日程</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-switch v-model="mine" active-text="仅看我的日程" @change="load" />
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ rows.length }} 条日程</el-tag>
      </div>
      <el-table :data="rows" v-loading="loading" size="default" empty-text="暂无日程">
        <el-table-column prop="title" label="日程标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="开始时间" width="165">
          <template #default="{ row }">{{ dt(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="165">
          <template #default="{ row }">{{ dt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="90">
          <template #default="{ row }">{{ scheduleType(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="location" label="地点" width="140" show-overflow-tooltip />
        <el-table-column prop="ownerName" label="负责人" width="95" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑日程' : '新建日程'" width="580px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="请输入日程标题" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="工作会议" value="WORK" />
            <el-option label="会议" value="MEETING" />
            <el-option label="客户拜访" value="VISIT" />
            <el-option label="现场巡查" value="INSPECT" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="地点"><el-input v-model="form.location" placeholder="请输入地点" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="计划中" value="PLAN" />
            <el-option label="已完成" value="DONE" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
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
const form = reactive<any>({ id: null, title: '', type: 'WORK', startTime: '', endTime: '', location: '', description: '', status: 'PLAN' })

function scheduleType(t: string) {
  return ({ WORK: '工作会议', MEETING: '会议', VISIT: '客户拜访', INSPECT: '现场巡查' } as any)[t] || t
}

async function load() {
  loading.value = true
  try {
    rows.value = await oaApi.schedules({ mine: mine.value })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, title: '', type: 'WORK', startTime: '', endTime: '', location: '', description: '', status: 'PLAN' })
  formVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, row)
  formVisible.value = true
}

async function save() {
  if (!form.title) {
    ElMessage.warning('请输入日程标题')
    return
  }
  saving.value = true
  try {
    if (form.id) await oaApi.updateSchedule(form.id, form)
    else await oaApi.createSchedule(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该日程吗？', '提示', { type: 'warning' })
  await oaApi.deleteSchedule(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
