<template>
  <div>
    <PageHeader title="部门管理" subtitle="组织树维护，部门负责人将作为审批流第一级审批人">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate(null)">新建一级部门</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <el-table :data="rows" v-loading="loading" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="name" label="部门名称" min-width="220" />
        <el-table-column prop="leaderName" label="负责人" width="110">
          <template #default="{ row }">{{ row.leaderName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="75" align="center" />
        <el-table-column label="状态" width="85" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">添加下级</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑部门' : '新建部门'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select v-model="form.parentId" :data="treeOptions" :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly :render-after-expand="false" clearable placeholder="留空为一级部门" style="width: 100%" />
        </el-form-item>
        <el-form-item label="部门名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.leaderId" filterable clearable style="width: 100%" placeholder="选择负责人">
            <el-option v-for="u in users" :key="u.id" :value="u.id" :label="u.name" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { systemApi } from '../../api'

const rows = ref<any[]>([])
const users = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const form = reactive<any>({})

// 编辑时排除自身，避免选择自己为上级
const treeOptions = computed(() => {
  const filter = (list: any[]): any[] =>
    list.filter((d) => d.id !== form.id).map((d) => ({ ...d, children: d.children ? filter(d.children) : [] }))
  return filter(rows.value)
})

async function load() {
  loading.value = true
  try {
    rows.value = await systemApi.deptTree()
  } finally {
    loading.value = false
  }
}

function openCreate(parent: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { name: '', parentId: parent ? parent.id : null, leaderId: null, sort: 0, status: 1 })
  formVisible.value = true
}

function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  delete form.children
  formVisible.value = true
}

async function save() {
  if (!form.name) return ElMessage.warning('请输入部门名称')
  saving.value = true
  try {
    if (form.id) await systemApi.updateDept(form.id, form)
    else await systemApi.createDept(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除部门「${row.name}」吗？`, '提示', { type: 'warning' })
  await systemApi.deleteDept(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  users.value = await systemApi.userOptions()
  load()
})
</script>
