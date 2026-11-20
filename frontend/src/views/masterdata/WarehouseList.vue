<template>
  <div>
    <PageHeader title="仓库档案" subtitle="仓库信息供进销存出入库与库存查询共用">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建仓库</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <el-table :data="rows" v-loading="loading" size="default">
        <el-table-column prop="code" label="仓库编号" width="110" />
        <el-table-column prop="name" label="仓库名称" width="160" />
        <el-table-column prop="location" label="地址" min-width="240" show-overflow-tooltip />
        <el-table-column prop="managerName" label="负责人" width="100" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑仓库' : '新建仓库'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="仓库名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.location" /></el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.managerId" filterable style="width: 100%">
            <el-option v-for="u in users" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
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
import { masterApi, systemApi } from '../../api'

const rows = ref<any[]>([])
const users = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const form = reactive<any>({})

async function load() {
  loading.value = true
  try {
    rows.value = await masterApi.warehouses()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { status: 1 })
  formVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  formVisible.value = true
}

async function save() {
  if (!form.name) {
    ElMessage.warning('请输入仓库名称')
    return
  }
  saving.value = true
  try {
    if (form.id) await masterApi.updateWarehouse(form.id, form)
    else await masterApi.createWarehouse(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该仓库吗？', '提示', { type: 'warning' })
  await masterApi.deleteWarehouse(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  users.value = await systemApi.userOptions()
  load()
})
</script>
