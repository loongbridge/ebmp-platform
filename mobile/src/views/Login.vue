<template>
  <div class="login">
    <div class="login-hero">
      <div class="logo">E</div>
      <div class="brand">企业商业管理平台</div>
      <div class="slogan">OA · SRM · CRM · 进销存 一体化移动办公</div>
    </div>

    <div class="login-card">
      <div class="field">
        <div class="field-label">账号</div>
        <input v-model="username" class="input" placeholder="请输入用户名" autocomplete="username" />
      </div>
      <div class="field">
        <div class="field-label">密码</div>
        <input v-model="password" class="input" type="password" placeholder="请输入密码" autocomplete="password" @keyup.enter="submit" />
      </div>

      <div class="server-toggle" @click="showServer = !showServer">
        <span>服务器地址</span>
        <span class="server-value">{{ apiBase || '当前站点（开发代理）' }}</span>
      </div>
      <div v-if="showServer" class="field" style="margin-top: 10px">
        <input v-model="apiBaseDraft" class="input" placeholder="如 http://192.168.1.10:8080" />
        <div class="hint">模拟器默认 10.0.2.2，真机请填电脑局域网 IP</div>
      </div>

      <button class="btn" :disabled="loading" style="margin-top: 18px" @click="submit">
        {{ loading ? '登录中…' : '登 录' }}
      </button>

      <div class="quick-title">快速体验账号</div>
      <div class="quick">
        <button v-for="a in accounts" :key="a.u" class="chip" @click="fill(a.u, a.p)">{{ a.n }}</button>
      </div>
    </div>

    <div v-if="toast" class="toast" :class="{ error: toastError }">{{ toast }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { auth } from '../stores/auth'
import { getApiBase, setApiBase } from '../utils/native'

const router = useRouter()
const route = useRoute()

const username = ref('admin')
const password = ref('Admin@123')
const loading = ref(false)
const showServer = ref(false)
const apiBase = ref(getApiBase())
const apiBaseDraft = ref(getApiBase())
const toast = ref('')
const toastError = ref(false)

const accounts = [
  { n: '管理员', u: 'admin', p: 'Admin@123' },
  { n: '总经理', u: 'gm', p: 'Gm@123' },
  { n: '销售', u: 'sales', p: 'Sales@123' },
  { n: '采购', u: 'buyer', p: 'Buyer@123' },
  { n: '仓管', u: 'wh', p: 'Wh@123' }
]

function fill(u: string, p: string) {
  username.value = u
  password.value = p
}

function tip(msg: string, error = false) {
  toast.value = msg
  toastError.value = error
  setTimeout(() => (toast.value = ''), 2200)
}

async function submit() {
  // 收起软键盘，避免键盘遮挡登录失败提示
  ;(document.activeElement as HTMLElement | null)?.blur()
  if (!username.value || !password.value) return tip('请输入账号与密码', true)
  if (apiBaseDraft.value && apiBaseDraft.value !== apiBase.value) {
    setApiBase(apiBaseDraft.value)
    apiBase.value = getApiBase()
  }
  loading.value = true
  try {
    await auth.login(username.value.trim(), password.value)
    const redirect = (route.query.redirect as string) || '/home'
    router.replace(redirect)
  } catch (e: any) {
    tip(e.message || '登录失败', true)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  background: linear-gradient(165deg, #17304e 0%, #1f4e8c 52%, #eef2f8 52%, #f4f6fa 100%);
  padding: calc(30px + env(safe-area-inset-top)) 20px 30px;
}

.login-hero {
  color: #fff;
  text-align: center;
  padding: 18px 0 26px;
}

.logo {
  width: 62px;
  height: 62px;
  margin: 0 auto 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 700;
}

.brand {
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 1px;
}

.slogan {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.76;
}

.login-card {
  background: #fff;
  border-radius: 20px;
  padding: 22px 18px;
  box-shadow: 0 14px 40px rgba(19, 37, 61, 0.16);
}

.server-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  padding: 10px 2px 0;
  font-size: 12px;
  color: var(--muted);
  border-top: 1px dashed var(--line);
}

.server-value {
  color: var(--primary);
  max-width: 65%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hint {
  font-size: 11px;
  color: var(--muted);
  margin-top: 6px;
  line-height: 1.6;
}

.quick-title {
  margin: 20px 0 10px;
  font-size: 12px;
  color: var(--muted);
  text-align: center;
}

.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.chip {
  background: #eef3fb;
  color: var(--primary);
  border-radius: 999px;
  padding: 7px 14px;
  font-size: 12px;
}

.chip:active {
  background: #dfe9f7;
}
</style>
