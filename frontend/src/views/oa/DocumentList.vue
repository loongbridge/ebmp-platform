<template>
  <div>
    <PageHeader title="文档中心" subtitle="制度文件、合同模板、方案与培训资料统一归档">
      <template #actions>
        <el-button type="primary" :icon="Upload" @click="openCreate">上传文档</el-button>
      </template>
    </PageHeader>

    <div class="page-card">
      <el-table :data="rows" v-loading="loading" size="default" empty-text="暂无文档">
        <el-table-column prop="name" label="文档名称" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.fileUrl" class="link-text" @click="download(row)">{{ row.name }}</span>
            <span v-else>{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="110">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.category }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="80" align="center" />
        <el-table-column prop="uploaderName" label="上传人" width="100" />
        <el-table-column label="大小" width="95" align="right">
          <template #default="{ row }">{{ fileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">{{ dt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" title="上传文档" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="选择文件">
          <el-upload :http-request="doUpload" :show-file-list="false" :headers="uploadHeaders">
            <el-button :icon="Upload">选择文件</el-button>
            <span v-if="form.fileUrl" style="margin-left: 10px; font-size: 12.5px; color: #2f8f5b">已上传：{{ form.name }}</span>
          </el-upload>
        </el-form-item>
        <el-form-item label="文档名称"><el-input v-model="form.name" placeholder="上传文件后自动带入，可修改" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="c in ['制度文件', '合同模板', '业务文档', '培训资料', '经营计划']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号"><el-input v-model="form.version" placeholder="如 v1.0" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import PageHeader from '../../components/PageHeader.vue'
import { oaApi } from '../../api'
import http from '../../api/http'
import { dt } from '../../utils/format'

const rows = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const form = reactive<any>({ name: '', category: '制度文件', version: 'v1.0', remark: '', fileUrl: '', filePath: '', fileSize: 0 })

const uploadHeaders = computed(() => ({ Authorization: `Bearer ${localStorage.getItem('ebmp_token') || ''}` }))

function fileSize(size: number) {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

async function load() {
  loading.value = true
  try {
    rows.value = await oaApi.documents()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { name: '', category: '制度文件', version: 'v1.0', remark: '', fileUrl: '', filePath: '', fileSize: 0 })
  formVisible.value = true
}

async function doUpload(options: any) {
  const fd = new FormData()
  fd.append('file', options.file)
  try {
    const data: any = await http.post('/files/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    form.name = data.name || data.name
    form.fileUrl = data.url
    form.filePath = data.url
    form.fileSize = data.size
    ElMessage.success('文件上传成功')
    options.onSuccess?.(data)
  } catch (e) {
    options.onError?.(e)
  }
}

async function save() {
  if (!form.name) {
    ElMessage.warning('请填写文档名称')
    return
  }
  saving.value = true
  try {
    await oaApi.createDocument(form)
    ElMessage.success('文档已归档')
    formVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该文档吗？', '提示', { type: 'warning' })
  await oaApi.deleteDocument(row.id)
  ElMessage.success('已删除')
  load()
}

function download(row: any) {
  if (row.fileUrl) window.open(row.fileUrl, '_blank')
}

onMounted(load)
</script>
