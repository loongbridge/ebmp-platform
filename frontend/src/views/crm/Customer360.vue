<template>
  <div>
    <PageHeader title="客户 360" subtitle="客户档案、联系人、商机、订单与跟进记录的全景视图">
      <template #actions>
        <el-button type="primary" @click="router.push('/master/customers')">客户档案管理</el-button>
      </template>
    </PageHeader>

    <div class="crm-360">
      <div class="page-card customer-list">
        <el-input v-model="keyword" placeholder="搜索客户名称 / 编号" clearable :prefix-icon="Search" style="margin-bottom: 10px" @input="filterCustomers" />
        <div class="customer-items">
          <div
            v-for="c in filteredCustomers"
            :key="c.id"
            class="customer-item"
            :class="{ active: current?.id === c.id }"
            @click="select(c)"
          >
            <div class="c-avatar">{{ c.name.slice(0, 1) }}</div>
            <div class="c-info">
              <b>{{ c.name }}</b>
              <span>{{ c.code }} · {{ c.industry || '未分类' }}</span>
            </div>
            <el-tag size="small" :type="c.level === 'A' ? 'danger' : c.level === 'B' ? 'primary' : 'info'" effect="plain">{{ c.level }}</el-tag>
          </div>
          <div v-if="filteredCustomers.length === 0" class="empty-box">暂无客户</div>
        </div>
      </div>

      <div class="page-card customer-detail">
        <div v-if="!current" class="empty-box" style="padding: 80px 0">请选择左侧客户查看 360 视图</div>
        <template v-else>
          <div class="c360-head">
            <div class="c-avatar" style="width: 48px; height: 48px; font-size: 20px">{{ current.name.slice(0, 1) }}</div>
            <div style="flex: 1">
              <h3 style="margin: 0 0 4px">{{ current.name }}</h3>
              <div class="muted" style="font-size: 12px">{{ current.code }} · {{ current.industry || '未分类' }} · 客户等级 {{ current.level }}</div>
            </div>
            <el-tag :type="current.status === 1 ? 'success' : 'info'" effect="plain">{{ current.status === 1 ? '合作中' : '已停用' }}</el-tag>
          </div>

          <el-tabs v-model="tab">
            <el-tab-pane label="基本信息" name="info">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="客户编号">{{ current.code }}</el-descriptions-item>
                <el-descriptions-item label="客户简称">{{ current.shortName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="所属行业">{{ current.industry || '-' }}</el-descriptions-item>
                <el-descriptions-item label="客户来源">{{ current.source || '-' }}</el-descriptions-item>
                <el-descriptions-item label="负责人">{{ current.ownerName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="最近联系">{{ dt(current.lastContactAt) }}</el-descriptions-item>
                <el-descriptions-item label="联系电话">{{ current.phone || '-' }}</el-descriptions-item>
                <el-descriptions-item label="邮箱">{{ current.email || '-' }}</el-descriptions-item>
                <el-descriptions-item label="地址" :span="2">{{ current.address || '-' }}</el-descriptions-item>
                <el-descriptions-item label="税号">{{ current.taxNo || '-' }}</el-descriptions-item>
                <el-descriptions-item label="授信额度">{{ money(current.creditLimit) }}</el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{ current.remark || '-' }}</el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>

            <el-tab-pane :label="`联系人（${contacts.length}）`" name="contacts">
              <el-button type="primary" size="small" :icon="Plus" style="margin-bottom: 10px" @click="openContact">新增联系人</el-button>
              <el-table :data="contacts" size="small">
                <el-table-column prop="name" label="姓名" width="100" />
                <el-table-column prop="position" label="职位" width="130" />
                <el-table-column prop="phone" label="电话" width="140" />
                <el-table-column prop="email" label="邮箱" min-width="180" />
                <el-table-column label="首要" width="70" align="center">
                  <template #default="{ row }"><el-tag v-if="row.isPrimary" size="small" type="success">是</el-tag><span v-else class="muted">-</span></template>
                </el-table-column>
                <el-table-column label="操作" width="80">
                  <template #default="{ row }"><el-button link type="danger" @click="removeContact(row)">删除</el-button></template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`商机（${opportunities.length}）`" name="opportunities">
              <el-table :data="opportunities" size="small">
                <el-table-column prop="oppNo" label="编号" width="130" />
                <el-table-column prop="name" label="商机名称" min-width="200" show-overflow-tooltip />
                <el-table-column label="阶段" width="95"><template #default="{ row }"><StatusTag :status="row.stage" /></template></el-table-column>
                <el-table-column label="金额" width="120" align="right"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
                <el-table-column prop="probability" label="赢率" width="70" align="center" />
                <el-table-column prop="ownerName" label="负责人" width="90" />
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`销售订单（${orders.length}）`" name="orders">
              <el-table :data="orders" size="small">
                <el-table-column prop="orderNo" label="订单编号" width="150" />
                <el-table-column label="下单日期" width="115"><template #default="{ row }">{{ d(row.orderDate) }}</template></el-table-column>
                <el-table-column label="金额" width="130" align="right"><template #default="{ row }">{{ money(row.totalAmount) }}</template></el-table-column>
                <el-table-column label="状态" width="100"><template #default="{ row }"><StatusTag :status="row.status" /></template></el-table-column>
                <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`跟进记录（${activities.length}）`" name="activities">
              <el-button type="primary" size="small" :icon="Plus" style="margin-bottom: 10px" @click="openActivity">新增跟进</el-button>
              <el-timeline>
                <el-timeline-item v-for="a in activities" :key="a.id" :timestamp="dt(a.activityTime)">
                  <b>{{ a.content }}</b>
                  <div class="muted" style="font-size: 12px">{{ a.ownerName || '-' }} · {{ activityType(a.type) }}<span v-if="a.nextPlan"> · 下一步：{{ a.nextPlan }}</span></div>
                </el-timeline-item>
              </el-timeline>
              <div v-if="activities.length === 0" class="empty-box">暂无跟进记录</div>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </div>

    <el-dialog v-model="contactVisible" title="新增联系人" width="520px">
      <el-form :model="contactForm" label-width="80px">
        <el-form-item label="姓名"><el-input v-model="contactForm.name" /></el-form-item>
        <el-form-item label="职位"><el-input v-model="contactForm.position" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="contactForm.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="contactForm.email" /></el-form-item>
        <el-form-item label="首要联系人"><el-switch v-model="contactForm.isPrimary" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="contactForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactVisible = false">取消</el-button>
        <el-button type="primary" @click="saveContact">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="activityVisible" title="新增跟进记录" width="520px">
      <el-form :model="activityForm" label-width="80px">
        <el-form-item label="跟进方式">
          <el-select v-model="activityForm.type" style="width: 100%">
            <el-option label="电话" value="CALL" />
            <el-option label="拜访" value="VISIT" />
            <el-option label="邮件" value="EMAIL" />
            <el-option label="会议" value="MEETING" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进内容"><el-input v-model="activityForm.content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="下一步"><el-input v-model="activityForm.nextPlan" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="activityVisible = false">取消</el-button>
        <el-button type="primary" @click="saveActivity">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusTag from '../../components/StatusTag.vue'
import { crmApi, erpApi, masterApi } from '../../api'
import { d, dt, money } from '../../utils/format'

const router = useRouter()
const keyword = ref('')
const allCustomers = ref<any[]>([])
const filteredCustomers = ref<any[]>([])
const current = ref<any>(null)
const contacts = ref<any[]>([])
const opportunities = ref<any[]>([])
const orders = ref<any[]>([])
const activities = ref<any[]>([])
const tab = ref('info')
const contactVisible = ref(false)
const activityVisible = ref(false)
const contactForm = reactive<any>({ customerId: null, name: '', position: '', phone: '', email: '', isPrimary: 0, remark: '' })
const activityForm = reactive<any>({ bizType: 'CUSTOMER', bizId: null, type: 'CALL', content: '', nextPlan: '' })

function activityType(t: string) {
  return ({ CALL: '电话', VISIT: '拜访', EMAIL: '邮件', MEETING: '会议' } as any)[t] || t
}

async function loadCustomers() {
  const data: any = await masterApi.customers({ page: 1, size: 100 })
  allCustomers.value = data.content || []
  filterCustomers()
}

function filterCustomers() {
  const kw = keyword.value.trim()
  filteredCustomers.value = kw
    ? allCustomers.value.filter((c) => [c.name, c.code, c.shortName].some((v) => String(v || '').includes(kw)))
    : allCustomers.value
}

async function select(c: any) {
  current.value = c
  tab.value = 'info'
  const [info, ct, opps, acts, orderList] = await Promise.all([
    masterApi.customer(c.id),
    masterApi.contacts(c.id),
    crmApi.opportunities(),
    crmApi.activitiesByBiz('CUSTOMER', c.id),
    erpApi.orders()
  ])
  current.value = info
  contacts.value = ct || []
  opportunities.value = (opps || []).filter((o: any) => o.customerId === c.id)
  activities.value = acts || []
  orders.value = (orderList || []).filter((o: any) => o.customerId === c.id)
}

function openContact() {
  Object.assign(contactForm, { customerId: current.value.id, name: '', position: '', phone: '', email: '', isPrimary: 0, remark: '' })
  contactVisible.value = true
}

async function saveContact() {
  if (!contactForm.name) {
    ElMessage.warning('请输入联系人姓名')
    return
  }
  await masterApi.createContact(contactForm)
  ElMessage.success('联系人已添加')
  contactVisible.value = false
  contacts.value = await masterApi.contacts(current.value.id)
}

async function removeContact(row: any) {
  await masterApi.deleteContact(row.id)
  ElMessage.success('已删除')
  contacts.value = await masterApi.contacts(current.value.id)
}

function openActivity() {
  Object.assign(activityForm, { bizType: 'CUSTOMER', bizId: current.value.id, type: 'CALL', content: '', nextPlan: '' })
  activityVisible.value = true
}

async function saveActivity() {
  if (!activityForm.content) {
    ElMessage.warning('请输入跟进内容')
    return
  }
  await crmApi.createActivity(activityForm)
  ElMessage.success('跟进记录已保存')
  activityVisible.value = false
  activities.value = await crmApi.activitiesByBiz('CUSTOMER', current.value.id)
}

onMounted(async () => {
  await loadCustomers()
  if (filteredCustomers.value.length) select(filteredCustomers.value[0])
})
</script>

<style scoped>
.crm-360 { display: grid; grid-template-columns: 320px 1fr; gap: 16px; align-items: start; }
.customer-list { padding: 14px; }
.customer-items { max-height: 620px; overflow-y: auto; }
.customer-item { display: flex; align-items: center; gap: 10px; padding: 9px 8px; border-radius: 8px; cursor: pointer; border: 1px solid transparent; }
.customer-item:hover { background: #f6f9fc; }
.customer-item.active { background: #eef5fc; border-color: #cfe1f5; }
.c-avatar { width: 34px; height: 34px; border-radius: 8px; background: linear-gradient(135deg, #3d7fd0, #6bb0ea); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 600; flex-shrink: 0; }
.c-info { flex: 1; min-width: 0; }
.c-info b { display: block; font-size: 12.5px; color: #22303f; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.c-info span { font-size: 11px; color: #93a4b7; }
.c360-head { display: flex; align-items: center; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid #eef2f7; margin-bottom: 12px; }
@media (max-width: 1200px) { .crm-360 { grid-template-columns: 1fr; } }
</style>
