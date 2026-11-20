<template>
  <div>
    <PageHeader title="用户管理" subtitle="平台账号、角色分配与组织归属管理">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建用户</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索用户名 / 姓名" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="query.deptId" placeholder="全部部门" clearable style="width: 180px" @change="search">
          <el-option v-for="d in depts" :key="d.id" :value="d.id" :label="d.name" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ total }} 个账号</el-tag>
      </div>

      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="username" label="用户名" width="110" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="deptName" label="部门" width="120" />
        <el-table-column prop="position" label="岗位" width="110" />
        <el-table-column prop="phone" label="手机号" width="125" />
        <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="r in row.roleNames || []" :key="r" size="small" effect="plain" style="margin-right: 4px">{{ r }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 14px">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" @size-change="search" />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑用户' : '新建用户'" width="600px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-select v-model="form.deptId" style="width: 100%" placeholder="选择部门">
                <el-option v-for="d in depts" :key="d.id" :value="d.id" :label="d.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="岗位"><el-input v-model="form.position" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item :label="form.id ? '重置密码' : '初始密码'">
              <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '留空则不修改' : '默认 123456'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%" placeholder="分配角色">
            <el-option v-for="r in roles" :key="r.id" :value="r.id" :label="r.name" />
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
import { systemApi } from '../../api'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const depts = ref<any[]>([])
const roles = ref<any[]>([])
const query = reactive<{ page: number; size: number; keyword: string; deptId: number | null }>({ page: 1, size: 10, keyword: '', deptId: null })
const form = reactive<any>({})

async function load() {
  loading.value = true
  try {
    const params: any = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.deptId) params.deptId = query.deptId
    const data: any = await systemApi.users(params)
    rows.value = data.content || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { username: '', realName: '', deptId: null, position: '', phone: '', email: '', password: '', status: 1, roleIds: [] })
  formVisible.value = true
}

function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, JSON.parse(JSON.stringify(row)), { password: '' })
  formVisible.value = true
}

async function save() {
  if (!form.username || !form.realName) return ElMessage.warning('请输入用户名与姓名')
  saving.value = true
  try {
    if (form.id) await systemApi.updateUser(form.id, form)
    else await systemApi.createUser(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除用户「${row.realName}」吗？`, '提示', { type: 'warning' })
  await systemApi.deleteUser(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  depts.value = await systemApi.deptOptions()
  roles.value = await systemApi.roleOptions()
  load()
})
</script>
