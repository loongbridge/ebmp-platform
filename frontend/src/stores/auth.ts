import { defineStore } from 'pinia'
import { authApi } from '../api'

interface UserInfo {
  id: number
  username: string
  realName: string
  deptName?: string
  roles: string[]
  permissions: string[]
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('ebmp_token') || '',
    user: JSON.parse(localStorage.getItem('ebmp_user') || 'null') as UserInfo | null
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    displayName: (state) => state.user?.realName || state.user?.username || '用户',
    roleNames: (state) => {
      const map: Record<string, string> = {
        ADMIN: '系统管理员', GM: '总经理', DEPT_MANAGER: '部门经理', SALES: '销售经理',
        BUYER: '采购专员', WAREHOUSE: '仓储管理员', HR: '人事专员', EMPLOYEE: '普通员工'
      }
      return (state.user?.roles || []).map((r) => map[r] || r).join(' / ')
    }
  },
  actions: {
    hasPerm(code: string) {
      if (!code) return true
      const perms = this.user?.permissions || []
      return perms.includes(code)
    },
    hasAnyPerm(codes: string[]) {
      if (!codes || codes.length === 0) return true
      const perms = this.user?.permissions || []
      return codes.some((c) => perms.includes(c))
    },
    async login(username: string, password: string) {
      const data = await authApi.login({ username, password })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('ebmp_token', data.token)
      localStorage.setItem('ebmp_user', JSON.stringify(data.user))
      return data
    },
    async refresh() {
      const user = await authApi.me()
      this.user = user
      localStorage.setItem('ebmp_user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('ebmp_token')
      localStorage.removeItem('ebmp_user')
    }
  }
})
