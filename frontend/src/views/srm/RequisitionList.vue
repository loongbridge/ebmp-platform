<template>
  <div>
    <PageHeader title="采购需求" subtitle="各部门发起采购申请，审批通过后可转采购订单">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建需求</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="applyFilter">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="DRAFT">草稿</el-radio-button>
          <el-radio-button value="PENDING">审批中</el-radio-button>
          <el-radio-button value="APPROVED">已通过</el-radio-button>
          <el-radio-button value="CONVERTED">已转订单</el-radio-button>
        </el-radio-group>
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ filtered.length }} 条</el-tag>
      </div>

      <el-table :data="filtered" v-loading="loading">
        <el-table-column prop="reqNo" label="需求单号" width="150" />
        <el-table-column prop="title" label="需求标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deptName" label="申请部门" width="110" />
        <el-table-column prop="applicantName" label="申请人" width="90" />
        <el-table-column prop="itemCount" label="明细" width="70" align="center" />
        <el-table-column label="预估金额" width="115" align="right">
          <template #default="{ row }">{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="needDate" label="需求日期" width="105" />
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">明细</el-button>
            <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="warning" @click="submit(row)">提交审批</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" title="新建采购需求" width="760px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="16"><el-form-item label="需求标题"><el-input v-model="form.title" placeholder="如：一季度原材料补货" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="需求日期"><el-date-picker v-model="form.needDate" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="采购明细">
          <el-table :data="form.items" size="small" border>
            <el-table-column label="物料" min-width="200">
              <template #default="{ row }">
                <el-select v-model="row.materialId" filterable placeholder="选择物料" style="width: 100%" @change="(v: number) => onMaterial(row, v)">
                  <el-option v-for="m in materials" :key="m.id" :value="m.id" :label="`${m.name}（${m.spec || '-'}）`" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="130">
              <template #default="{ row }"><el-input-number v-model="row.quantity" :min="1" style="width: 100%" @change="calc" /></template>
            </el-table-column>
            <el-table-column label="预估单价" width="140">
              <template #default="{ row }"><el-input-number v-model="row.estPrice" :min="0" style="width: 100%" @change="calc" /></template>
            </el-table-column>
            <el-table-column label="小计" width="110" align="right">
              <template #default="{ row }">{{ money((row.quantity || 0) * (row.estPrice || 0)) }}</template>
            </el-table-column>
            <el-table-column label="" width="60" align="center">
              <template #default="{ $index }"><el-button link type="danger" @click="form.items.splice($index, 1); calc()">删除</el-button></template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 8px; display: flex; justify-content: space-between; align-items: center">
            <el-button size="small" :icon="Plus" @click="form.items.push({ materialId: null, quantity: 1, estPrice: 0 })">添加明细</el-button>
            <span style="font-weight: 600">合计：{{ money(form.totalAmount) }}</span>
          </div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="`需求明细 - ${detail?.reqNo || ''}`" width="640px">
      <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
        <el-descriptions-item label="需求标题">{{ detail?.title }}</el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :status="detail?.status" /></el-descriptions-item>
        <el-descriptions-item label="申请部门">{{ detail?.deptName }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail?.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="需求日期">{{ detail?.needDate }}</el-descriptions-item>
        <el-descriptions-item label="预估金额">{{ money(detail?.totalAmount) }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.items || []" size="small" border>
        <el-table-column prop="materialName" label="物料" min-width="160" />
        <el-table-column prop="spec" label="规格" width="100" />
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column prop="unit" label="单位" width="60" align="center" />
        <el-table-column label="预估单价" width="100" align="right">
          <template #default="{ row }">{{ money(row.estPrice) }}</template>
        </el-table-column>
        <el-table-column label="小计" width="110" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusTag from '../../components/StatusTag.vue'
import { masterApi, srmApi } from '../../api'
import { money } from '../../utils/format'

const rows = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const statusFilter = ref('')
const formVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<any>(null)
const materials = ref<any[]>([])
const form = reactive<any>({ title: '', needDate: '', remark: '', items: [], totalAmount: 0 })

const filtered = computed(() => (statusFilter.value ? rows.value.filter((r) => r.status === statusFilter.value) : rows.value))

async function load() {
  loading.value = true
  try {
    rows.value = await srmApi.requisitions()
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  if (!materials.value.length) materials.value = await masterApi.materialOptions()
  Object.assign(form, { title: '', needDate: '', remark: '', items: [{ materialId: null, quantity: 1, estPrice: 0 }], totalAmount: 0 })
  formVisible.value = true
}

function onMaterial(item: any, materialId: number) {
  const m = materials.value.find((x) => x.id === materialId)
  if (m) item.estPrice = m.costPrice || m.salePrice || 0
  calc()
}

function calc() {
  form.items.forEach((i: any) => (i.amount = (i.quantity || 0) * (i.estPrice || 0)))
  form.totalAmount = form.items.reduce((s: number, i: any) => s + (i.amount || 0), 0)
}

async function save() {
  if (!form.title) return ElMessage.warning('请输入需求标题')
  if (!form.items.length || form.items.some((i: any) => !i.materialId)) return ElMessage.warning('请完善采购明细')
  calc()
  saving.value = true
  try {
    await srmApi.createRequisition(form)
    ElMessage.success('已保存草稿，可在列表中提交审批')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function submit(row: any) {
  await ElMessageBox.confirm(`确定提交「${row.title}」进入审批流吗？`, '提交审批', { type: 'warning' })
  await srmApi.submitRequisition(row.id)
  ElMessage.success('已提交审批')
  load()
}

async function openDetail(row: any) {
  detail.value = await srmApi.requisition(row.id)
  detailVisible.value = true
}

onMounted(load)
</script>
