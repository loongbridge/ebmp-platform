import { computed, reactive, ref } from 'vue'
import { authApi } from '../api'
import { clearToken, getToken, setToken } from '../utils/native'

const user = ref<any>(null)
const token = ref(getToken())

const state = reactive({ user, token })

export const auth = {
  state,
  isLoggedIn: computed(() => !!token.value),
  displayName: computed(() => state.user?.realName || state.user?.username || ''),
  perms: computed<string[]>(() => state.user?.permissions || []),

  hasPerm(code: string) {
    const list = state.user?.permissions
    if (!list || !list.length) return true
    return list.includes(code)
  },

  async login(username: string, password: string) {
    const data: any = await authApi.login({ username, password })
    token.value = data.token
    setToken(data.token)
    state.user = data.user
  },

  async refresh() {
    if (!token.value) return
    try {
      state.user = await authApi.me()
    } catch {
      /* token 失效由拦截器处理 */
    }
  },

  logout() {
    token.value = ''
    state.user = null
    clearToken()
  }
}
