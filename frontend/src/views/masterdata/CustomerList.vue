<template>
  <div>
    <PageHeader title="客户档案" subtitle="平台统一主数据：客户信息供 CRM、ERP 销售订单与合同中心共用">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建客户</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索客户名称 / 编号" clearable style="width: 240px" @keyup.enter="search" />
        <el-select v-model="query.level" placeholder="客户等级" clearable style="width: 120px" @change="search">
          <el-option label="A 级" value="A" /><el-option label="B 级" value="B" /><el-option label="C 级" value="C" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <div class="spacer"></div>
        <el-tag effect="plain" type="primary">共 {{ total }} 家客户</el-tag>
      </div>

      <el-table :data="rows" v-loading="loading" size="default">
        <el-table-column prop="code" label="客户编号" width="110" />
        <el-table-column prop="name" label="客户名称" min-width="220" show-overflow-tooltip>
          <template #default="{ row }"><span class="link-text" @click="router.push('/crm/customers')">{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column prop="industry" label="行业" width="110" />
        <el-table-column label="等级" width="75" align="center">
          <template #default="{ row }"><el-tag size="small" :type="row.level === 'A' ? 'danger' : row.level === 'B' ? 'primary' : 'info'" effect="plain">{{ row.level }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column prop="ownerName" label="负责人" width="90" />
        <el-table-column prop="phone" label="电话" width="135" />
        <el-table-column label="授信额度" width="120" align="right">
          <template #default="{ row }">{{ money(row.creditLimit) }}</template>
        </el-table-column>
        <el-table-column label="最近联系" width="155">
          <template #default="{ row }">{{ dt(row.lastContactAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openContacts(row)">联系人</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 14px">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" @size-change="search" />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑客户' : '新建客户'" width="660px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="客户名称"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="客户简称"><el-input v-model="form.shortName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所属行业"><el-input v-model="form.industry" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="客户等级">
              <el-select v-model="form.level" style="width: 100%">
                <el-option label="A 级（重点）" value="A" /><el-option label="B 级（普通）" value="B" /><el-option label="C 级（观察）" value="C" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="客户来源"><el-input v-model="form.source" placeholder="如：展会 / 渠道推荐" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-select v-model="form.ownerId" filterable style="width: 100%">
                <el-option v-for="u in users" :key="u.id" :label="u.name" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="税号"><el-input v-model="form.taxNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="授信额度"><el-input-number v-model="form.creditLimit" :min="0" :step="50000" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="地址"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="contactDrawer" :title="`联系人 - ${current?.name}`" size="520px">
      <el-button type="primary" size="small" :icon="Plus" style="margin-bottom: 12px" @click="openContactForm">新增联系人</el-button>
      <el-table :data="contacts" size="small">
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="position" label="职位" width="110" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="150" />
        <el-table-column label="首要" width="60" align="center">
          <template #default="{ row }"><el-tag v-if="row.isPrimary" size="small" type="success">是</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ row }"><el-button link type="danger" @click="removeContact(row)">删除</el-button></template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="contactVisible" title="新增联系人" width="460px" append-to-body>
        <el-form :model="contactForm" label-width="80px">
          <el-form-item label="姓名"><el-input v-model="contactForm.name" /></el-form-item>
          <el-form-item label="职位"><el-input v-model="contactForm.position" /></el-form-item>
          <el-form-item label="电话"><el-input v-model="contactForm.phone" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="contactForm.email" /></el-form-item>
          <el-form-item label="首要联系人"><el-switch v-model="contactForm.isPrimary" :active-value="1" :inactive-value="0" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="contactVisible = false">取消</el-button>
          <el-button type="primary" @click="saveContact">保存</el-button>
        </template>
      </el-dialog>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { masterApi, systemApi } from '../../api'
import { dt, money } from '../../utils/format'

const router = useRouter()
const rows = ref<any[]>([])
const users = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const contactDrawer = ref(false)
const contactVisible = ref(false)
const current = ref<any>(null)
const contacts = ref<any[]>([])
const query = reactive({ page: 1, size: 10, keyword: '', level: '' })
const form = reactive<any>({})
const contactForm = reactive<any>({})

async function load() {
  loading.value = true
  try {
    const data: any = await masterApi.customers({ ...query })
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
  Object.assign(form, { level: 'B', source: '', status: 1, creditLimit: 0, ownerId: users.value[0]?.id })
  formVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  formVisible.value = true
}

async function save() {
  if (!form.name) {
    ElMessage.warning('请输入客户名称')
    return
  }
  saving.value = true
  try {
    if (form.id) await masterApi.updateCustomer(form.id, form)
    else await masterApi.createCustomer(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该客户吗？其联系人将一并删除。', '提示', { type: 'warning' })
  await masterApi.deleteCustomer(row.id)
  ElMessage.success('已删除')
  load()
}

async function openContacts(row: any) {
  current.value = row
  contactDrawer.value = true
  contacts.value = await masterApi.contacts(row.id)
}

function openContactForm() {
  Object.keys(contactForm).forEach((k) => delete contactForm[k])
  Object.assign(contactForm, { customerId: current.value.id, name: '', position: '', phone: '', email: '', isPrimary: 0 })
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

onMounted(async () => {
  users.value = await systemApi.userOptions()
  load()
})
</script>
