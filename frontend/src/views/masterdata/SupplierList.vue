<template>
  <div>
    <PageHeader title="供应商档案" subtitle="平台统一主数据：供应商信息供 SRM 采购、合同中心共用">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建供应商</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索供应商名称 / 编号" clearable style="width: 240px" @keyup.enter="search" />
        <el-input v-model="query.category" placeholder="品类，如：钢材" clearable style="width: 150px" @keyup.enter="search" />
        <el-button type="primary" @click="search">查询</el-button>
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ total }} 家供应商</el-tag>
      </div>

      <el-table :data="rows" v-loading="loading" size="default">
        <el-table-column prop="code" label="编号" width="100" />
        <el-table-column prop="name" label="供应商名称" min-width="210" show-overflow-tooltip />
        <el-table-column prop="category" label="主营品类" width="100" />
        <el-table-column label="等级" width="75" align="center">
          <template #default="{ row }"><el-tag size="small" :type="row.level === 'A' ? 'danger' : row.level === 'B' ? 'primary' : 'info'" effect="plain">{{ row.level }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="score" label="绩效分" width="85" align="right" />
        <el-table-column prop="contactName" label="联系人" width="90" />
        <el-table-column prop="phone" label="电话" width="135" />
        <el-table-column label="合作开始" width="110">
          <template #default="{ row }">{{ d(row.cooperationStart) }}</template>
        </el-table-column>
        <el-table-column prop="purchaseOrderCount" label="订单数" width="80" align="center" />
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑供应商' : '新建供应商'" width="660px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="供应商名称"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="主营品类"><el-input v-model="form.category" placeholder="如：钢材 / 水泥" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="等级">
              <el-select v-model="form.level" style="width: 100%">
                <el-option label="A 级" value="A" /><el-option label="B 级" value="B" /><el-option label="C 级" value="C" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="开户银行"><el-input v-model="form.bankName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="银行账号"><el-input v-model="form.bankAccount" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="税号"><el-input v-model="form.taxNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="合作开始"><el-date-picker v-model="form.cooperationStart" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="地址"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { masterApi } from '../../api'
import { d } from '../../utils/format'

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
    const data: any = await masterApi.suppliers({ ...query })
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
  Object.assign(form, { level: 'B', status: 1 })
  formVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  formVisible.value = true
}

async function save() {
  if (!form.name) {
    ElMessage.warning('请输入供应商名称')
    return
  }
  saving.value = true
  try {
    if (form.id) await masterApi.updateSupplier(form.id, form)
    else await masterApi.createSupplier(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该供应商吗？', '提示', { type: 'warning' })
  await masterApi.deleteSupplier(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
