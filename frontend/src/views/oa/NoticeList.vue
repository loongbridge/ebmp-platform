<template>
  <div>
    <PageHeader title="公告管理" subtitle="企业公告、制度与通知的统一发布渠道">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreate">发布公告</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <el-table :data="rows" v-loading="loading" size="default">
        <el-table-column prop="title" label="公告标题" min-width="260" show-overflow-tooltip>
          <template #default="{ row }"><span class="link-text" @click="openView(row)">{{ row.title }}</span></template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="110">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.category || '通知' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布人" width="100" />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ dt(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column prop="views" label="浏览" width="70" align="center" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openView(row)">查看</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 14px">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑公告' : '发布公告'" width="640px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="请输入公告标题" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="c in ['通知公告', '经营动态', '制度规范', '培训通知', '行政通知', '企业文化', '运营管理', '系统通知']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewVisible" :title="current?.title" width="640px">
      <div style="font-size: 12.5px; color: #8496aa; margin-bottom: 12px">
        {{ current?.publisherName }} · {{ dt(current?.publishTime) }} · {{ current?.category }}
      </div>
      <div style="line-height: 1.9; font-size: 13.5px; white-space: pre-wrap">{{ current?.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { oaApi } from '../../api'
import { dt } from '../../utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })
const formVisible = ref(false)
const viewVisible = ref(false)
const saving = ref(false)
const current = ref<any>(null)
const form = reactive<any>({ id: null, title: '', category: '通知公告', content: '', status: 1 })

async function load() {
  loading.value = true
  try {
    const data: any = await oaApi.notices({ ...query })
    rows.value = data.content || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, { id: null, title: '', category: '通知公告', content: '', status: 1 })
}

function openCreate() {
  resetForm()
  formVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, row)
  formVisible.value = true
}

function openView(row: any) {
  current.value = row
  viewVisible.value = true
}

async function save() {
  if (!form.title) {
    ElMessage.warning('请输入公告标题')
    return
  }
  saving.value = true
  try {
    if (form.id) await oaApi.updateNotice(form.id, form)
    else await oaApi.createNotice(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该公告吗？', '提示', { type: 'warning' })
  await oaApi.deleteNotice(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
