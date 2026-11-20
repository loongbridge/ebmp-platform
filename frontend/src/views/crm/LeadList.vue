<template>
  <div>
    <PageHeader title="线索管理" subtitle="从线索获取、跟进到转化为客户与商机的完整闭环">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建线索</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">线索总量</div>
        <div class="kpi-value">{{ stats.leadCount || 0 }}<small>条</small></div>
        <div class="kpi-foot">新线索 <em>{{ stats.newLeadCount || 0 }}</em> 条</div>
        <el-icon class="kpi-icon"><Aim /></el-icon>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">客户总数</div>
        <div class="kpi-value">{{ stats.customerCount || 0 }}<small>家</small></div>
        <div class="kpi-foot">线索转化客户持续沉淀</div>
        <el-icon class="kpi-icon"><UserFilled /></el-icon>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">商机数量</div>
        <div class="kpi-value">{{ stats.opportunityCount || 0 }}<small>个</small></div>
        <div class="kpi-foot">在跟金额 {{ money(stats.openAmount) }}</div>
        <el-icon class="kpi-icon"><TrendCharts /></el-icon>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">赢单率</div>
        <div class="kpi-value">{{ stats.winRate || 0 }}<small>%</small></div>
        <div class="kpi-foot">赢单金额 {{ money(stats.wonAmount) }}</div>
        <el-icon class="kpi-icon"><Trophy /></el-icon>
      </div>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索线索名称 / 公司 / 联系人" clearable style="width: 260px" />
        <el-select v-model="status" placeholder="线索状态" clearable style="width: 130px">
          <el-option label="新线索" value="NEW" />
          <el-option label="跟进中" value="FOLLOWING" />
          <el-option label="已确认" value="QUALIFIED" />
          <el-option label="已转化" value="CONVERTED" />
          <el-option label="无效" value="INVALID" />
        </el-select>
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ filtered.length }} 条</el-tag>
      </div>

      <el-table :data="filtered" v-loading="loading" size="default" empty-text="暂无线索">
        <el-table-column prop="leadNo" label="线索编号" width="120" />
        <el-table-column prop="name" label="线索名称" min-width="190" show-overflow-tooltip />
        <el-table-column prop="company" label="关联公司" min-width="160" show-overflow-tooltip />
        <el-table-column prop="contactName" label="联系人" width="90" />
        <el-table-column prop="phone" label="电话" width="125" />
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column prop="ownerName" label="负责人" width="90" />
        <el-table-column label="下次跟进" width="155">
          <template #default="{ row }">{{ dt(row.nextFollowAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'CONVERTED'" link type="success" @click="convert(row)">转化</el-button>
            <el-button link type="primary" @click="openFollow(row)">跟进</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑线索' : '新建线索'" width="620px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="线索名称"><el-input v-model="form.name" placeholder="如：无锡太湖新城项目" /></el-form-item>
        <el-form-item label="关联公司"><el-input v-model="form.company" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="来源">
          <el-select v-model="form.source" style="width: 100%">
            <el-option v-for="s in ['展会', '官网咨询', '渠道推荐', '老客户推荐', '电话开发', '其他']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.ownerId" filterable style="width: 100%">
            <el-option v-for="u in users" :key="u.id" :label="`${u.name}（${u.deptName || '-'}）`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.id" label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="新线索" value="NEW" />
            <el-option label="跟进中" value="FOLLOWING" />
            <el-option label="已确认" value="QUALIFIED" />
            <el-option label="无效" value="INVALID" />
          </el-select>
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker v-model="form.nextFollowAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="followVisible" title="记录跟进" width="520px">
      <div class="muted" style="margin-bottom: 12px">线索：{{ currentLead?.name }}</div>
      <el-form :model="followForm" label-width="80px">
        <el-form-item label="跟进方式">
          <el-select v-model="followForm.type" style="width: 100%">
            <el-option label="电话" value="CALL" />
            <el-option label="拜访" value="VISIT" />
            <el-option label="邮件" value="EMAIL" />
            <el-option label="会议" value="MEETING" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进内容">
          <el-input v-model="followForm.content" type="textarea" :rows="4" placeholder="记录沟通内容与客户反馈" />
        </el-form-item>
        <el-form-item label="下一步">
          <el-input v-model="followForm.nextPlan" placeholder="下一步跟进计划" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveFollow">保存跟进</el-button>
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
import { crmApi, systemApi } from '../../api'
import { dt, money } from '../../utils/format'

const rows = ref<any[]>([])
const users = ref<any[]>([])
const stats = ref<any>({})
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const status = ref('')
const formVisible = ref(false)
const followVisible = ref(false)
const currentLead = ref<any>(null)
const form = reactive<any>({ id: null, name: '', company: '', contactName: '', phone: '', email: '', source: '展会', ownerId: null, status: 'NEW', nextFollowAt: '', remark: '' })
const followForm = reactive<any>({ bizType: 'LEAD', bizId: null, type: 'CALL', content: '', nextPlan: '' })

const filtered = computed(() =>
  rows.value.filter((r) => {
    const kw = keyword.value.trim()
    const matchKw = !kw || [r.name, r.company, r.contactName, r.leadNo].some((v) => String(v || '').includes(kw))
    const matchStatus = !status.value || r.status === status.value
    return matchKw && matchStatus
  })
)

async function load() {
  loading.value = true
  try {
    const [list, st] = await Promise.all([crmApi.leads(), crmApi.stats()])
    rows.value = list
    stats.value = st
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, name: '', company: '', contactName: '', phone: '', email: '', source: '展会', ownerId: users.value[0]?.id, status: 'NEW', nextFollowAt: '', remark: '' })
  formVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, row)
  formVisible.value = true
}

async function save() {
  if (!form.name) {
    ElMessage.warning('请输入线索名称')
    return
  }
  saving.value = true
  try {
    if (form.id) await crmApi.updateLead(form.id, form)
    else await crmApi.createLead(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function convert(row: any) {
  await ElMessageBox.confirm(`确定将线索「${row.name}」转化为客户并创建商机吗？`, '线索转化', { type: 'warning' })
  const opp: any = await crmApi.convertLead(row.id)
  ElMessage.success(`转化成功，已生成商机 ${opp.oppNo}`)
  load()
}

function openFollow(row: any) {
  currentLead.value = row
  Object.assign(followForm, { bizType: 'LEAD', bizId: row.id, type: 'CALL', content: '', nextPlan: '' })
  followVisible.value = true
}

async function saveFollow() {
  if (!followForm.content) {
    ElMessage.warning('请输入跟进内容')
    return
  }
  saving.value = true
  try {
    await crmApi.createActivity(followForm)
    ElMessage.success('跟进记录已保存')
    followVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该线索吗？', '提示', { type: 'warning' })
  await crmApi.deleteLead(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  users.value = await systemApi.userOptions()
  load()
})
</script>
