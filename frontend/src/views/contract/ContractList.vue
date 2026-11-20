<template>
  <div>
    <PageHeader title="合同中心" subtitle="客户与供应商合同统一管理，提交后走统一审批流，通过即生效">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建合同</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-radio-group v-model="typeFilter" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="CUSTOMER">客户合同</el-radio-button>
          <el-radio-button value="SUPPLIER">供应商合同</el-radio-button>
        </el-radio-group>
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ rows.length }} 份合同</el-tag>
      </div>

      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="contractNo" label="合同编号" width="150" />
        <el-table-column prop="title" label="合同标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="105" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'CUSTOMER' ? 'primary' : 'warning'" effect="plain">
              {{ row.type === 'CUSTOMER' ? '客户合同' : '供应商合同' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="partyName" label="签约方" min-width="150" show-overflow-tooltip />
        <el-table-column label="合同金额" width="120" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="期限" width="195" align="center">
          <template #default="{ row }">{{ row.startDate || '-' }} ~ {{ row.endDate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="85" />
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="warning" @click="submit(row)">提交审批</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" title="新建合同" width="620px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="合同类型">
          <el-radio-group v-model="form.type" @change="form.partyId = null">
            <el-radio value="CUSTOMER">客户合同</el-radio>
            <el-radio value="SUPPLIER">供应商合同</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="签约方">
          <el-select v-model="form.partyId" filterable style="width: 100%" :placeholder="form.type === 'CUSTOMER' ? '选择客户' : '选择供应商'">
            <el-option v-for="p in partyOptions" :key="p.id" :value="p.id" :label="p.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同标题"><el-input v-model="form.title" placeholder="如：2026 年度框架采购合同" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="合同金额"><el-input-number v-model="form.amount" :min="0" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同期限">
              <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" style="width: 100%"
                start-placeholder="开始日期" end-placeholder="结束日期" @change="onRange" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
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
import { contractApi, masterApi } from '../../api'
import { money } from '../../utils/format'

const rows = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const typeFilter = ref('')
const formVisible = ref(false)
const customers = ref<any[]>([])
const suppliers = ref<any[]>([])
const dateRange = ref<[string, string] | null>(null)
const form = reactive<any>({})

const partyOptions = computed(() => (form.type === 'CUSTOMER' ? customers.value : suppliers.value))

async function load() {
  loading.value = true
  try {
    rows.value = await contractApi.list(typeFilter.value ? { type: typeFilter.value } : {})
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  if (!customers.value.length) customers.value = await masterApi.customerOptions()
  if (!suppliers.value.length) suppliers.value = await masterApi.supplierOptions()
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { type: 'CUSTOMER', partyId: null, title: '', amount: 0, startDate: '', endDate: '', remark: '' })
  dateRange.value = null
  formVisible.value = true
}

function onRange(v: [string, string] | null) {
  form.startDate = v?.[0] || ''
  form.endDate = v?.[1] || ''
}

async function save() {
  if (!form.partyId || !form.title) return ElMessage.warning('请选择签约方并填写合同标题')
  saving.value = true
  try {
    await contractApi.create(form)
    ElMessage.success('已保存草稿，可在列表中提交审批')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function submit(row: any) {
  await ElMessageBox.confirm(`确定提交合同「${row.title}」进入审批流吗？合同需总经理审批。`, '提交审批', { type: 'warning' })
  await contractApi.submit(row.id)
  ElMessage.success('已提交审批')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该合同草稿吗？', '提示', { type: 'warning' })
  await contractApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
