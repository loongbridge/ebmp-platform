<template>
  <div class="app">
    <router-view />
    <nav v-if="showTab" class="tabbar">
      <router-link v-for="t in tabs" :key="t.path" :to="t.path" class="tab" :class="{ active: route.path === t.path }">
        <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
          <path :d="t.icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round" />
        </svg>
        <span>{{ t.name }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { auth } from './stores/auth'

const route = useRoute()

// 冷启动时用本地令牌拉取当前用户，保证各页面都能拿到角色与用户信息
onMounted(() => auth.refresh())

const tabs = [
  { path: '/home', name: '首页', icon: 'M3 10.5 12 3l9 7.5M5 9.5V21h14V9.5' },
  { path: '/approvals', name: '审批', icon: 'M8 4h8v3H8zM6 4H5a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-1M7 13l3 3 7-7' },
  { path: '/stock', name: '库存', icon: 'M3 7l9-4 9 4-9 4zM3 7v10l9 4 9-4V7M12 11v10' },
  { path: '/me', name: '我的', icon: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21c0-4 3.6-6 8-6s8 2 8 6' }
]

const showTab = computed(() => route.path !== '/login')
</script>
