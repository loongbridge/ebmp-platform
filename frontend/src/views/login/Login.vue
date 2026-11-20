<template>
  <div class="login-page">
    <div class="login-left">
      <div class="login-brand">
        <div class="mark">E</div>
        <div>
          <b>企业商业管理平台</b>
          <span>ENTERPRISE BUSINESS MANAGEMENT PLATFORM</span>
        </div>
      </div>
      <div class="login-slogan">
        <h1>一套平台，贯通<br />办公、采购、库存与客户经营</h1>
        <p>整合 OA 协同办公、SRM 供应商协同、进销存仓储与 CRM 客户关系管理，构建统一组织权限、统一主数据、统一审批流与统一经营驾驶舱。</p>
      </div>
      <div class="login-modules">
        <span>OA 协同办公</span>
        <span>SRM 供应商协同</span>
        <span>进销存仓储</span>
        <span>CRM 客户关系</span>
        <span>统一审批引擎</span>
        <span>经营驾驶舱</span>
      </div>
    </div>
    <div class="login-right">
      <div class="login-form-wrap">
        <h2>欢迎登录</h2>
        <div class="sub">请使用企业账号登录统一工作台</div>
        <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
          <el-form-item label="账号">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名" clearable>
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" @keyup.enter="handleLogin">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button type="primary" size="large" style="width: 100%; margin-top: 6px" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>

        <div class="login-accounts">
          <div><b>快速体验：</b>admin / Admin@123（系统管理员）</div>
          <div><b>总经理：</b>gm / Gm@123　<b>销售：</b>sales / Sales@123</div>
          <div><b>采购：</b>buyer / Buyer@123　<b>仓储：</b>wh / Wh@123</div>
          <div style="margin-top: 6px">
            <el-tag v-for="acc in quickAccounts" :key="acc.username" size="small" style="margin-right: 6px; cursor: pointer" @click="useAccount(acc.username, acc.password)">
              {{ acc.label }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'Admin@123' })
const quickAccounts = [
  { label: '管理员', username: 'admin', password: 'Admin@123' },
  { label: '总经理', username: 'gm', password: 'Gm@123' },
  { label: '销售经理', username: 'sales', password: 'Sales@123' },
  { label: '采购经理', username: 'buyer', password: 'Buyer@123' },
  { label: '仓储主管', username: 'wh', password: 'Wh@123' }
]

function useAccount(username: string, password: string) {
  form.username = username
  form.password = password
}

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功，欢迎 ' + auth.displayName)
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch {
    /* interceptor 已提示 */
  } finally {
    loading.value = false
  }
}
</script>
