<template>
  <div>
    <div class="topbar">
      <div class="topbar-row">
        <div>
          <div class="topbar-title">公司公告</div>
          <div class="topbar-sub">已加载 {{ list.length }} 条公告</div>
        </div>
        <button class="back-link" @click="reload">刷新</button>
      </div>
    </div>

    <div class="page">
      <div v-if="loading && !list.length" class="loading">加载中…</div>
      <div v-else-if="!list.length" class="empty">
        <div class="empty-icon">!</div>
        暂无公告
      </div>

      <div v-else class="list">
        <div v-for="n in list" :key="n.id" class="card" @click="current = n">
          <div class="card-top">
            <div class="card-title">{{ n.title }}</div>
            <span v-if="n.category" class="tag">{{ n.category }}</span>
          </div>
          <div class="card-meta">
            <span>{{ n.publisherName || '管理员' }}</span>
            <span>{{ dt(n.publishTime) }}</span>
            <span>阅读 {{ n.views || 0 }}</span>
          </div>
          <div class="notice-brief">{{ plain(n.content) }}</div>
        </div>
        <button class="btn ghost" :disabled="loading || noMore" style="margin-top: 4px" @click="loadMore">
          {{ noMore ? '没有更多了' : loading ? '加载中…' : '加载更多' }}
        </button>
      </div>

      <div v-if="toast" class="toast" :class="{ error: toastError }">{{ toast }}</div>
    </div>

    <div v-if="current" class="mask" @click.self="current = null">
      <div class="sheet">
        <div class="sheet-head">
          <div class="sheet-title">{{ current.title }}</div>
          <button class="sheet-close" @click="current = null">关闭</button>
        </div>
        <div class="card-meta" style="margin-bottom: 10px">
          <span>{{ current.publisherName || '管理员' }}</span>
          <span>{{ dt(current.publishTime) }}</span>
          <span v-if="current.category">{{ current.category }}</span>
        </div>
        <div class="notice-content">{{ current.content }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { oaApi } from '../api'
import { dt } from '../utils/format'

const list = ref<any[]>([])
const page = ref(1)
const size = 10
const noMore = ref(false)
const loading = ref(false)
const current = ref<any>(null)
const toast = ref('')
const toastError = ref(false)

const plain = (v?: string) => String(v || '').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').slice(0, 80)

function tip(msg: string, error = false) {
  toast.value = msg
  toastError.value = error
  setTimeout(() => (toast.value = ''), 2200)
}

async function fetchPage() {
  loading.value = true
  try {
    const res: any = await oaApi.notices({ page: page.value, size })
    const content = res.list || res.content || []
    list.value = page.value === 1 ? content : list.value.concat(content)
    noMore.value = list.value.length >= (res.total || 0) || content.length === 0
  } catch (e: any) {
    if (page.value > 1) page.value -= 1
    tip(e.message || '加载失败', true)
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  noMore.value = false
  fetchPage()
}

function loadMore() {
  if (noMore.value || loading.value) return
  page.value += 1
  fetchPage()
}

onMounted(fetchPage)
</script>

<style scoped>
.notice-brief {
  margin-top: 8px;
  font-size: 12px;
  color: var(--muted);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-content {
  font-size: 14px;
  line-height: 1.85;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text);
}
</style>
