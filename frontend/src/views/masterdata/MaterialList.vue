<template>
  <div>
    <PageHeader title="物料档案" subtitle="平台统一主数据：物料信息供采购、销售与库存模块共用">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建物料</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索物料名称 / 编号" clearable style="width: 240px" @keyup.enter="search" />
        <el-input v-model="query.category" placeholder="品类" clearable style="width: 140px" @keyup.enter="search" />
        <el-button type="primary" @click="search">查询</el-button>
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ total }} 个物料</el-tag>
      </div>

      <el-table :data="rows" v-loading="loading" size="default">
        <el-table-column prop="code" label="物料编号" width="105" />
        <el-table-column prop="name" label="物料名称" min-width="210" show-overflow-tooltip />
        <el-table-column prop="category" label="品类" width="95" />
        <el-table-column prop="spec" label="规格" width="110" />
        <el-table-column prop="unit" label="单位" width="65" align="center" />
        <el-table-column label="销售价" width="100" align="right">
          <template #default="{ row }">{{ money(row.salePrice) }}</template>
        </el-table-column>
        <el-table-column label="成本价" width="100" align="right">
          <template #default="{ row }">{{ money(row.costPrice) }}</template>
        </el-table-column>
        <el-table-column prop="safeStock" label="安全库存" width="90" align="right" />
        <el-table-column prop="stockQuantity" label="当前库存" width="90" align="right" />
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑物料' : '新建物料'" width="600px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="物料名称"><el-input v-model="form.name" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="品类"><el-input v-model="form.category" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规格"><el-input v-model="form.spec" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="单位"><el-input v-model="form.unit" placeholder="如：吨 / 米 / 台" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="安全库存"><el-input-number v-model="form.safeStock" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="销售价"><el-input-number v-model="form.salePrice" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="成本价"><el-input-number v-model="form.costPrice" :min="0" style="width: 100%" /></el-form-item></el-col>
        </el-row>
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
import { masterApi } from '../../api'
import { money } from '../../utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', category: '' })
const form = reactive<any>({})

async function load() {
  loading.value = true
  try {
    const data: any = await masterApi.materials({ ...query })
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
  Object.assign(form, { unit: '吨', status: 1, safeStock: 0, salePrice: 0, costPrice: 0 })
  formVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  formVisible.value = true
}

async function save() {
  if (!form.name || !form.unit) {
    ElMessage.warning('请输入物料名称与单位')
    return
  }
  saving.value = true
  try {
    if (form.id) await masterApi.updateMaterial(form.id, form)
    else await masterApi.createMaterial(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该物料吗？', '提示', { type: 'warning' })
  await masterApi.deleteMaterial(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
