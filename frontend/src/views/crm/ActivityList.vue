<template>
  <div>
    <PageHeader title="跟进记录" subtitle="线索、客户与商机的全量沟通记录，支撑销售过程管理">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增跟进</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-select v-model="filterType" placeholder="跟进方式" clearable style="width: 130px">
          <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-select v-model="filterBiz" placeholder="关联对象" clearable style="width: 140px">
          <el-option label="线索" value="LEAD" />
          <el-option label="客户" value="CUSTOMER" />
          <el-option label="商机" value="OPPORTUNITY" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索跟进内容" clearable style="width: 240px" />
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ filtered.length }} 条记录</el-tag>
      </div>

      <el-table :data="filtered" v-loading="loading" size="default" empty-text="暂无跟进记录">
        <el-table-column label="跟进方式" width="100" align="center">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ typeLabel(row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="关联对象" width="110">
          <template #default="{ row }">{{ bizLabel(row.bizType) }}</template>
        </el-table-column>
        <el-table-column prop="bizName" label="对象名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="跟进内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="nextPlan" label="下一步计划" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ownerName" label="跟进人" width="90" />
        <el-table-column label="跟进时间" width="160">
          <template #default="{ row }">{{ dt(row.activityTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }"><el-button link type="danger" @click="remove(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" title="新增跟进记录" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="关联对象">
          <el-radio-group v-model="form.bizType" @change="form.bizId = null">
            <el-radio value="LEAD">线索</el-radio>
            <el-radio value="CUSTOMER">客户</el-radio>
            <el-radio value="OPPORTUNITY">商机</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="选择对象">
          <el-select v-model="form.bizId" filterable style="width: 100%" placeholder="请选择关联对象">
            <el-option v-for="item in bizOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进方式">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="记录沟通内容、客户反馈与关键信息" />
        </el-form-item>
        <el-form-item label="下一步"><el-input v-model="form.nextPlan" placeholder="下一步跟进计划" /></el-form-item>
        <el-form-item label="跟进时间">
          <el-date-picker v-model="form.activityTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { crmApi, masterApi } from '../../api'
import { dt } from '../../utils/format'

const typeOptions = [
  { value: 'CALL', label: '电话' }, { value: 'VISIT', label: '拜访' },
  { value: 'EMAIL', label: '邮件' }, { value: 'MEETING', label: '会议' }, { value: 'OTHER', label: '其他' }
]

const rows = ref<any[]>([])
const leads = ref<any[]>([])
const customers = ref<any[]>([])
const opportunities = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const filterType = ref('')
const filterBiz = ref('')
const keyword = ref('')
const form = reactive<any>({ bizType: 'CUSTOMER', bizId: null, type: 'CALL', content: '', nextPlan: '', activityTime: '' })

const filtered = computed(() =>
  rows.value.filter((r) => {
    const kw = keyword.value.trim()
    return (!filterType.value || r.type === filterType.value)
      && (!filterBiz.value || r.bizType === filterBiz.value)
      && (!kw || String(r.content || '').includes(kw) || String(r.bizName || '').includes(kw))
  })
)

const bizOptions = computed(() => {
  if (form.bizType === 'LEAD') return leads.value.map((l) => ({ id: l.id, name: l.name + (l.company ? ' - ' + l.company : '') }))
  if (form.bizType === 'OPPORTUNITY') return opportunities.value.map((o) => ({ id: o.id, name: o.name }))
  return customers.value.map((c) => ({ id: c.id, name: c.name }))
})

function typeLabel(t: string) {
  return typeOptions.find((x) => x.value === t)?.label || t
}

function bizLabel(t: string) {
  return ({ LEAD: '线索', CUSTOMER: '客户', OPPORTUNITY: '商机' } as any)[t] || t
}

async function load() {
  loading.value = true
  try {
    const [list, l, c, o] = await Promise.all([crmApi.activities(), crmApi.leads(), masterApi.customerOptions(), crmApi.opportunities()])
    rows.value = list
    leads.value = l
    customers.value = c
    opportunities.value = o
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { bizType: 'CUSTOMER', bizId: null, type: 'CALL', content: '', nextPlan: '', activityTime: '' })
  formVisible.value = true
}

async function save() {
  if (!form.bizId || !form.content) {
    ElMessage.warning('请选择关联对象并填写跟进内容')
    return
  }
  saving.value = true
  try {
    await crmApi.createActivity(form)
    ElMessage.success('跟进记录已保存')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该跟进记录吗？', '提示', { type: 'warning' })
  await crmApi.deleteActivity(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
