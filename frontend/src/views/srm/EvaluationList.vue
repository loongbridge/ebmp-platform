<template>
  <div>
    <PageHeader title="供应商评价" subtitle="按周期对供应商进行质量、交期、服务、价格四维评分，自动更新供应商等级">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建评价</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="supplierName" label="供应商" min-width="170" show-overflow-tooltip />
        <el-table-column prop="period" label="评价周期" width="100" align="center" />
        <el-table-column prop="qualityScore" label="质量" width="75" align="center" />
        <el-table-column prop="deliveryScore" label="交期" width="75" align="center" />
        <el-table-column prop="serviceScore" label="服务" width="75" align="center" />
        <el-table-column prop="priceScore" label="价格" width="75" align="center" />
        <el-table-column label="综合得分" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.totalScore >= 90 ? 'success' : row.totalScore >= 75 ? 'primary' : 'danger'" effect="dark">{{ row.totalScore }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="evaluatorName" label="评价人" width="90" />
        <el-table-column prop="comment" label="评语" min-width="160" show-overflow-tooltip />
        <el-table-column label="评价时间" width="155">
          <template #default="{ row }">{{ dt(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" title="新建供应商评价" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="供应商">
          <el-select v-model="form.supplierId" filterable style="width: 100%" placeholder="选择供应商">
            <el-option v-for="s in suppliers" :key="s.id" :value="s.id" :label="s.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="评价周期">
          <el-date-picker v-model="form.period" type="month" value-format="YYYY-MM" style="width: 100%" placeholder="选择月份" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="质量得分">
              <el-input-number v-model="form.qualityScore" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="交期得分">
              <el-input-number v-model="form.deliveryScore" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务得分">
              <el-input-number v-model="form.serviceScore" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格得分">
              <el-input-number v-model="form.priceScore" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert type="info" :closable="false" style="margin-bottom: 12px"
          :title="`预计综合得分：${preview}（≥90 为 A 级，≥75 为 B 级，其余 C 级）`" />
        <el-form-item label="评语"><el-input v-model="form.comment" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { masterApi, srmApi } from '../../api'
import { dt } from '../../utils/format'

const rows = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const suppliers = ref<any[]>([])
const form = reactive<any>({})

const preview = computed(() =>
  (((form.qualityScore || 0) + (form.deliveryScore || 0) + (form.serviceScore || 0) + (form.priceScore || 0)) / 4).toFixed(1)
)

async function load() {
  loading.value = true
  try {
    rows.value = await srmApi.evaluations()
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  if (!suppliers.value.length) suppliers.value = await masterApi.supplierOptions()
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { supplierId: null, period: '', qualityScore: 80, deliveryScore: 80, serviceScore: 80, priceScore: 80, comment: '' })
  formVisible.value = true
}

async function save() {
  if (!form.supplierId || !form.period) return ElMessage.warning('请选择供应商与评价周期')
  saving.value = true
  try {
    await srmApi.createEvaluation(form)
    ElMessage.success('评价已提交，供应商等级已更新')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
