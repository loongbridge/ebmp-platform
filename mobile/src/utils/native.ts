const DEFAULT_API_BASE = 'http://10.0.2.2:8080'

interface NativeBridge {
  getApiBase(): string
  setApiBase(v: string): void
  getToken(): string
  setToken(v: string): void
  clearToken(): void
  toast?(v: string): void
}

declare global {
  interface Window {
    NativeConfig?: NativeBridge
  }
}

/** 是否运行在 Android 原生壳内 */
export const isNative = typeof window !== 'undefined' && !!window.NativeConfig

/** 当前接口服务地址；浏览器开发态返回空串，走 Vite 代理 */
export function getApiBase(): string {
  if (window.NativeConfig) return window.NativeConfig.getApiBase() || DEFAULT_API_BASE
  return localStorage.getItem('ebmp_api_base') || ''
}

export function setApiBase(base: string) {
  const v = (base || '').trim().replace(/\/+$/, '')
  if (window.NativeConfig) window.NativeConfig.setApiBase(v)
  else localStorage.setItem('ebmp_api_base', v)
}

export function getToken(): string {
  if (window.NativeConfig) return window.NativeConfig.getToken() || ''
  return localStorage.getItem('ebmp_token') || ''
}

export function setToken(token: string) {
  if (window.NativeConfig) window.NativeConfig.setToken(token)
  else localStorage.setItem('ebmp_token', token)
}

export function clearToken() {
  if (window.NativeConfig) window.NativeConfig.clearToken()
  else localStorage.removeItem('ebmp_token')
}

export { DEFAULT_API_BASE }
