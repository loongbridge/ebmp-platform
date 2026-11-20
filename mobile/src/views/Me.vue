<template>
  <div>
    <div class="topbar">
      <div class="me-head">
        <div class="avatar">{{ initial }}</div>
        <div style="flex: 1">
          <div class="me-name">{{ user.realName || user.username || '未登录' }}</div>
          <div class="me-sub">{{ user.deptName || '未分配部门' }} · {{ (user.roles || []).join(' / ') || '普通员工' }}</div>
        </div>
      <button class="back-link" @click="load">刷新</button>
      </div>
    </div>

    <div class="page">
      <div class="pull-card" style="margin-top: -24px">
        <div class="section-head" style="margin-bottom: 10px">
          <div class="section-title">今日考勤</div>
          <span class="tag" :class="todayRecord && todayRecord.checkInTime ? 'ok' : 'gray'">
            {{ todayRecord && todayRecord.checkInTime ? '已打卡' : '未打卡' }}
          </span>
        </div>
        <div class="detail-row">
          <span class="detail-key">上班打卡</span>
          <span class="detail-val">{{ todayRecord && todayRecord.checkInTime ? dt(todayRecord.checkInTime) : '—' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-key">下班打卡</span>
          <span class="detail-val">{{ todayRecord && todayRecord.checkOutTime ? dt(todayRecord.checkOutTime) : '—' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-key">考勤状态</span>
          <span class="detail-val">
            <span v-if="todayRecord" class="tag" :class="statusMeta(todayRecord.status).cls">{{ statusMeta(todayRecord.status).label }}</span>
            <span v-else>—</span>
          </span>
        </div>
        <div class="detail-row">
          <span class="detail-key">公司今日出勤</span>
          <span class="detail-val">{{ summary.checkedIn || 0 }} / {{ summary.totalUsers || 0 }} 人（迟到 {{ summary.late || 0 }}）</span>
        </div>
        <div class="row-btns">
          <button class="btn ghost" :disabled="busy || !!(todayRecord && todayRecord.checkInTime)" @click="punch('in')">上班打卡</button>
          <button class="btn" :disabled="busy || !(todayRecord && todayRecord.checkInTime) || !!(todayRecord && todayRecord.checkOutTime)" @click="punch('out')">下班打卡</button>
        </div>
      </div>

      <div class="menu">
        <button class="menu-item" @click="go('/approvals')">
          <span>待办审批</span>
          <span class="menu-value">›</span>
        </button>
        <button class="menu-item" @click="go('/tasks')">
          <span>我的任务</span>
          <span class="menu-value">›</span>
        </button>
        <button class="menu-item" @click="go('/leads')">
          <span>销售线索</span>
          <span class="menu-value">›</span>
        </button>
        <button class="menu-item" @click="go('/stock')">
          <span>库存查询</span>
          <span class="menu-value">›</span>
        </button>
        <button class="menu-item" @click="openServer">
          <span>服务器地址</span>
          <span class="menu-value">{{ apiBase || (isNative ? DEFAULT_API_BASE : '当前站点') }}</span>
        </button>
        <button class="menu-item" @click="showAbout = true">
          <span>关于</span>
          <span class="menu-value">EBMP 移动端 v1.0.0</span>
        </button>
      </div>

      <button class="btn danger" style="margin-top: 16px" @click="logout">退出登录</button>

      <div v-if="toast" class="toast" :class="{ error: toastError }">{{ toast }}</div>
    </div>

    <div v-if="serverSheet" class="mask" @click.self="serverSheet = false">
      <div class="sheet">
        <div class="sheet-head">
          <div class="sheet-title">服务器地址</div>
          <button class="sheet-close" @click="serverSheet = false">关闭</button>
        </div>
        <div class="field">
          <div class="field-label">接口服务地址（不含 /api）</div>
          <input v-model="apiDraft" class="input" placeholder="如 http://192.168.1.10:8080" />
          <div class="hint">模拟器可用 10.0.2.2，真机请填写电脑局域网 IP；修改后需重新登录。</div>
        </div>
        <div class="row-btns">
          <button class="btn ghost" @click="serverSheet = false">取消</button>
          <button class="btn" @click="saveServer">保存</button>
        </div>
      </div>
    </div>

    <div v-if="showAbout" class="mask" @click.self="showAbout = false">
      <div class="sheet">
        <div class="sheet-head">
          <div class="sheet-title">关于 EBMP 移动端</div>
          <button class="sheet-close" @click="showAbout = false">关闭</button>
        </div>
        <div class="detail-row"><span class="detail-key">应用名称</span><span class="detail-val">企业商业管理平台</span></div>
        <div class="detail-row"><span class="detail-key">版本</span><span class="detail-val">1.0.0</span></div>
        <div class="detail-row"><span class="detail-key">运行环境</span><span class="detail-val">{{ isNative ? 'Android 客户端' : '移动浏览器' }}</span></div>
        <div class="detail-row"><span class="detail-key">功能范围</span><span class="detail-val">OA / SRM / CRM / 进销存</span></div>
        <div class="hint" style="text-align: center; margin-top: 14px">© 2026 企业商业管理平台</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { oaApi } from '../api'
import { auth } from '../stores/auth'
import { dt, statusMeta } from '../utils/format'
import { DEFAULT_API_BASE, getApiBase, isNative, setApiBase } from '../utils/native'

const router = useRouter()
const user = computed<any>(() => auth.state.user || {})
const initial = computed(() => (user.value.realName || user.value.username || 'E').slice(0, 1).toUpperCase())
const apiBase = ref(getApiBase())
const apiDraft = ref(getApiBase())
const serverSheet = ref(false)
const showAbout = ref(false)
const summary = ref<any>({})
const todayRecord = ref<any>(null)
const busy = ref(false)
const toast = ref('')
const toastError = ref(false)

function tip(msg: string, error = false) {
  toast.value = msg
  toastError.value = error
  setTimeout(() => (toast.value = ''), 2200)
}

async function load() {
  try {
    await auth.refresh()
    const [s, mine] = await Promise.all([oaApi.attendanceSummary(), oaApi.attendance({ mine: true })])
    summary.value = s
    const raw = String((s as any).date || '')
    todayRecord.value = (mine as any[]).find((a) => String(a.workDate || '').slice(0, 10) === raw) || null
  } catch (e: any) {
    tip(e.message || '加载失败', true)
  }
}

async function punch(kind: 'in' | 'out') {
  busy.value = true
  try {
    if (kind === 'in') await oaApi.checkIn()
    else await oaApi.checkOut()
    tip(kind === 'in' ? '上班打卡成功' : '下班打卡成功')
    await load()
  } catch (e: any) {
    tip(e.message || '打卡失败', true)
  } finally {
    busy.value = false
  }
}

function go(path: string) {
  router.push(path)
}

function openServer() {
  apiDraft.value = getApiBase()
  serverSheet.value = true
}

function saveServer() {
  const draft = apiDraft.value.trim()
  if (draft && !/^https?:\/\/\S+$/i.test(draft)) {
    tip('地址需以 http:// 或 https:// 开头', true)
    return
  }
  const old = getApiBase()
  setApiBase(draft)
  apiBase.value = getApiBase()
  serverSheet.value = false
  if (apiBase.value !== old) {
    auth.logout()
    tip('服务器地址已更新，请重新登录')
    setTimeout(() => router.replace('/login'), 600)
  } else {
    tip('服务器地址未变化')
  }
}

function logout() {
  auth.logout()
  router.replace('/login')
}

onMounted(load)
</script>
