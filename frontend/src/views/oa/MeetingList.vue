<template>
  <div>
    <PageHeader title="会议管理" subtitle="会议室预定、参会人与会议纪要协同">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">安排会议</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <el-table :data="rows" v-loading="loading" size="default" empty-text="暂无会议">
        <el-table-column prop="title" label="会议主题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="room" label="会议室" width="150" show-overflow-tooltip />
        <el-table-column label="开始时间" width="165">
          <template #default="{ row }">{{ dt(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="165">
          <template #default="{ row }">{{ dt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="organizerName" label="组织人" width="90" />
        <el-table-column label="参会人" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="n in row.attendeeNames" :key="n" size="small" effect="plain" style="margin: 2px 4px 2px 0">{{ n }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="finish(row)">结束</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑会议' : '安排会议'" width="620px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="会议主题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="会议室"><el-input v-model="form.room" placeholder="如：总部 1 号会议室" /></el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="参会人">
          <el-select v-model="form.attendeeIds" multiple filterable style="width: 100%" placeholder="选择参会人">
            <el-option v-for="u in users" :key="u.id" :label="`${u.name}（${u.deptName || '-'}）`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="已安排" value="SCHEDULED" />
            <el-option label="进行中" value="ONGOING" />
            <el-option label="已结束" value="DONE" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
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
import { oaApi, systemApi } from '../../api'
import { dt } from '../../utils/format'

const rows = ref<any[]>([])
const users = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const form = reactive<any>({ id: null, title: '', room: '', startTime: '', endTime: '', attendeeIds: [], status: 'SCHEDULED', remark: '' })

async function load() {
  loading.value = true
  try {
    rows.value = await oaApi.meetings()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, title: '', room: '', startTime: '', endTime: '', attendeeIds: [], status: 'SCHEDULED', remark: '' })
  formVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, { ...row, attendeeIds: [...(row.attendeeIds || [])] })
  formVisible.value = true
}

async function save() {
  if (!form.title) {
    ElMessage.warning('请输入会议主题')
    return
  }
  saving.value = true
  try {
    if (form.id) await oaApi.updateMeeting(form.id, form)
    else await oaApi.createMeeting(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function finish(row: any) {
  await oaApi.updateMeeting(row.id, { ...row, status: 'DONE', attendeeIds: row.attendeeIds || [] })
  ElMessage.success('会议已结束')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该会议吗？', '提示', { type: 'warning' })
  await oaApi.deleteMeeting(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  users.value = await systemApi.userOptions()
  load()
})
</script>
