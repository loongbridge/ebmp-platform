<template>
  <div>
    <PageHeader title="任务管理" subtitle="个人任务与团队任务协同，支持优先级与到期跟踪">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建任务</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-switch v-model="query.mine" active-text="仅看我的任务" @change="load" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load">
          <el-option label="待开始" value="TODO" />
          <el-option label="进行中" value="DOING" />
          <el-option label="已完成" value="DONE" />
        </el-select>
        <div class="spacer"></div>
        <el-tag type="warning" effect="plain">未完成 {{ pendingCount }} 项</el-tag>
      </div>

      <el-table :data="filtered" v-loading="loading" size="default" empty-text="暂无任务">
        <el-table-column prop="title" label="任务标题" min-width="240" show-overflow-tooltip />
        <el-table-column prop="description" label="任务说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="assigneeName" label="负责人" width="95" />
        <el-table-column prop="creatorName" label="创建人" width="95" />
        <el-table-column label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="(priorityMeta[row.priority] || priorityMeta.NORMAL).type as any" effect="plain">
              {{ (priorityMeta[row.priority] || priorityMeta.NORMAL).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="截止日期" width="110">
          <template #default="{ row }">{{ d(row.dueDate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'DONE'" link type="success" @click="complete(row)">完成</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑任务' : '新建任务'" width="580px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="任务标题"><el-input v-model="form.title" placeholder="请输入任务标题" /></el-form-item>
        <el-form-item label="任务说明"><el-input v-model="form.description" type="textarea" :rows="4" placeholder="描述任务目标与要求" /></el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.assigneeId" filterable style="width: 100%" placeholder="选择负责人">
            <el-option v-for="u in users" :key="u.id" :label="`${u.name}（${u.deptName || '-'}）`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" placeholder="选择截止日期" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="form.priority">
            <el-radio value="HIGH">高</el-radio>
            <el-radio value="NORMAL">中</el-radio>
            <el-radio value="LOW">低</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="待开始" value="TODO" />
            <el-option label="进行中" value="DOING" />
            <el-option label="已完成" value="DONE" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusTag from '../../components/StatusTag.vue'
import { oaApi, systemApi } from '../../api'
import { d, priorityMeta } from '../../utils/format'

const rows = ref<any[]>([])
const users = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const query = reactive({ mine: false, status: '' })
const form = reactive<any>({ id: null, title: '', description: '', assigneeId: null, dueDate: '', priority: 'NORMAL', status: 'TODO' })

const filtered = computed(() => rows.value)
const pendingCount = computed(() => rows.value.filter((r) => r.status !== 'DONE').length)

async function load() {
  loading.value = true
  try {
    rows.value = await oaApi.tasks({ mine: query.mine, status: query.status || undefined })
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  users.value = await systemApi.userOptions()
}

function openCreate() {
  Object.assign(form, { id: null, title: '', description: '', assigneeId: users.value[0]?.id, dueDate: '', priority: 'NORMAL', status: 'TODO' })
  formVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, row)
  formVisible.value = true
}

async function save() {
  if (!form.title) {
    ElMessage.warning('请输入任务标题')
    return
  }
  saving.value = true
  try {
    if (form.id) await oaApi.updateTask(form.id, form)
    else await oaApi.createTask(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function complete(row: any) {
  await oaApi.completeTask(row.id)
  ElMessage.success('任务已完成')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该任务吗？', '提示', { type: 'warning' })
  await oaApi.deleteTask(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  await loadUsers()
  load()
})
</script>
