<template>
  <div>
    <PageHeader title="角色权限" subtitle="定义角色并分配功能权限码，控制菜单与接口访问">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建角色</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="code" label="角色编码" width="130" />
        <el-table-column prop="name" label="角色名称" width="130" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="权限数" width="90" align="center">
          <template #default="{ row }">{{ (row.permissionIds || []).length }}</template>
        </el-table-column>
        <el-table-column label="状态" width="85" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑 / 授权</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? `编辑角色 - ${form.name}` : '新建角色'" width="620px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="角色编码"><el-input v-model="form.code" :disabled="!!form.id" placeholder="如 SALES" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="角色名称"><el-input v-model="form.name" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="功能权限">
          <div style="width: 100%; border: 1px solid var(--el-border-color); border-radius: 6px; padding: 10px 14px; max-height: 260px; overflow: auto">
            <el-checkbox-group v-model="form.permissionIds">
              <el-checkbox v-for="p in permissions" :key="p.id" :value="p.id" style="display: flex; margin-right: 0">
                {{ p.name }}<span style="color: #909399; font-size: 12px; margin-left: 6px">{{ p.code }}</span>
              </el-checkbox>
            </el-checkbox-group>
          </div>
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
import { systemApi } from '../../api'

const rows = ref<any[]>([])
const permissions = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const form = reactive<any>({})

async function load() {
  loading.value = true
  try {
    rows.value = await systemApi.roles()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { code: '', name: '', description: '', status: 1, permissionIds: [] })
  formVisible.value = true
}

function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  form.permissionIds = [...(row.permissionIds || [])]
  formVisible.value = true
}

async function save() {
  if (!form.code || !form.name) return ElMessage.warning('请输入角色编码与名称')
  saving.value = true
  try {
    if (form.id) await systemApi.updateRole(form.id, form)
    else await systemApi.createRole(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除角色「${row.name}」吗？`, '提示', { type: 'warning' })
  await systemApi.deleteRole(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  permissions.value = await systemApi.permissions()
  load()
})
</script>
