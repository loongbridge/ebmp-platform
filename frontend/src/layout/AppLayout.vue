<template>
  <div class="app-shell">
    <aside class="app-aside" :class="{ collapsed }">
      <div class="brand">
        <div class="brand-logo">E</div>
        <div class="brand-text" v-show="!collapsed">
          <b>企业商业管理平台</b>
          <span>OA · SRM · ERP · CRM</span>
        </div>
      </div>
      <el-menu
        class="side-menu"
        :default-active="route.path"
        :collapse="collapsed"
        :collapse-transition="false"
        background-color="transparent"
        text-color="#b7c7db"
        active-text-color="#ffffff"
        router
      >
        <template v-for="section in visibleSections" :key="section.key">
          <el-sub-menu v-if="section.children && section.children.length" :index="section.key">
            <template #title>
              <el-icon><component :is="section.icon" /></el-icon>
              <span>{{ section.title }}</span>
            </template>
            <el-menu-item v-for="item in section.children" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="section.path">
            <el-icon><component :is="section.icon" /></el-icon>
            <template #title>{{ section.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </aside>

    <div class="app-body">
      <header class="app-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" /><Expand v-else />
          </el-icon>
          <div>
            <div class="header-title">{{ route.meta.title || '工作台' }}</div>
            <div class="header-breadcrumb">{{ moduleName }} / {{ route.meta.title }}</div>
          </div>
        </div>
        <div class="header-right">
          <span class="header-clock">{{ clock }}</span>
          <el-badge :value="todoCount" :hidden="todoCount === 0" :max="99">
            <el-icon style="font-size: 19px; cursor: pointer; color: #5b6b7f" @click="router.push('/workflow/todo')"><Bell /></el-icon>
          </el-badge>
          <el-dropdown trigger="click">
            <div class="header-user">
              <div class="avatar">{{ (auth.displayName || 'U').slice(0, 1) }}</div>
              <div class="user-meta">
                <b>{{ auth.displayName }}</b>
                <span>{{ auth.user?.deptName || '总部' }} · {{ auth.roleNames || '员工' }}</span>
              </div>
              <el-icon style="color: #8b9bae"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ auth.user?.username }}</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { dashboardApi } from '../api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const todoCount = ref(0)
const clock = ref('')
let timer: any = null

const sections = [
  {
    key: 'portal', title: '门户工作台', icon: 'Odometer', perm: 'dashboard:view',
    children: [
      { path: '/dashboard', title: '经营驾驶舱', icon: 'DataBoard', perm: 'dashboard:view' },
      { path: '/workflow/todo', title: '统一待办', icon: 'Bell' },
      { path: '/workflow/approvals', title: '审批中心', icon: 'Checked', perm: 'workflow:approve' }
    ]
  },
  {
    key: 'oa', title: '协同办公 OA', icon: 'Suitcase', perm: 'oa:view',
    children: [
      { path: '/oa/notices', title: '公告管理', icon: 'Document' },
      { path: '/oa/tasks', title: '任务管理', icon: 'List' },
      { path: '/oa/schedules', title: '日程安排', icon: 'Calendar' },
      { path: '/oa/meetings', title: '会议管理', icon: 'ChatDotRound' },
      { path: '/oa/leaves', title: '请假管理', icon: 'Sunny' },
      { path: '/oa/attendance', title: '考勤打卡', icon: 'AlarmClock' },
      { path: '/oa/documents', title: '文档中心', icon: 'FolderOpened' }
    ]
  },
  {
    key: 'crm', title: '客户关系 CRM', icon: 'UserFilled', perm: 'crm:view',
    children: [
      { path: '/crm/leads', title: '线索管理', icon: 'Aim' },
      { path: '/crm/opportunities', title: '商机管理', icon: 'TrendCharts' },
      { path: '/crm/customers', title: '客户 360', icon: 'User' },
      { path: '/crm/activities', title: '跟进记录', icon: 'ChatLineRound' },
      { path: '/master/customers', title: '客户档案', icon: 'Tickets' }
    ]
  },
  {
    key: 'srm', title: '供应商协同 SRM', icon: 'ShoppingCart', perm: 'srm:view',
    children: [
      { path: '/srm/requisitions', title: '采购需求', icon: 'DocumentAdd' },
      { path: '/srm/orders', title: '采购订单', icon: 'ShoppingCart' },
      { path: '/master/suppliers', title: '供应商档案', icon: 'OfficeBuilding' },
      { path: '/srm/evaluations', title: '供应商评价', icon: 'Medal' },
      { path: '/srm/performance', title: '供应商绩效', icon: 'DataLine' }
    ]
  },
  {
    key: 'erp', title: '进销存仓储', icon: 'Box', perm: 'erp:view',
    children: [
      { path: '/erp/orders', title: '销售订单', icon: 'Money' },
      { path: '/erp/stock', title: '库存查询', icon: 'Goods' },
      { path: '/erp/stock-alerts', title: '库存预警', icon: 'Warning' },
      { path: '/erp/ledger', title: '出入库台账', icon: 'Files' },
      { path: '/master/materials', title: '物料档案', icon: 'Collection' },
      { path: '/master/warehouses', title: '仓库档案', icon: 'HomeFilled' }
    ]
  },
  {
    key: 'contract', title: '合同中心', icon: 'Stamp', path: '/contracts', permAny: ['crm:view', 'srm:view']
  },
  {
    key: 'system', title: '系统管理', icon: 'Setting', perm: 'system:view',
    children: [
      { path: '/system/users', title: '用户管理', icon: 'Avatar' },
      { path: '/system/roles', title: '角色权限', icon: 'Lock' },
      { path: '/system/depts', title: '部门管理', icon: 'OfficeBuilding' }
    ]
  }
]

const visibleSections = computed(() => {
  return sections
    .map((s: any) => {
      if (s.children) {
        const children = s.children.filter((c: any) => !c.perm || auth.hasPerm(c.perm))
        return { ...s, children }
      }
      return s
    })
    .filter((s: any) => {
      if (s.permAny) return auth.hasAnyPerm(s.permAny)
      if (s.perm) return auth.hasPerm(s.perm)
      return true
    })
})

const moduleName = computed(() => {
  const p = route.path
  if (p.startsWith('/oa')) return '协同办公 OA'
  if (p.startsWith('/crm')) return '客户关系 CRM'
  if (p.startsWith('/srm')) return '供应商协同 SRM'
  if (p.startsWith('/erp')) return '进销存仓储'
  if (p.startsWith('/master')) return '主数据管理'
  if (p.startsWith('/system')) return '系统管理'
  if (p.startsWith('/contracts')) return '合同中心'
  if (p.startsWith('/workflow')) return '审批中心'
  return '门户工作台'
})

function tick() {
  const now = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][now.getDay()]
  clock.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} 星期${week} ${now.toLocaleTimeString('zh-CN', { hour12: false })}`
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(async () => {
  tick()
  timer = setInterval(tick, 1000)
  try {
    const todos: any = await dashboardApi.todos()
    todoCount.value = todos.total || 0
  } catch {
    todoCount.value = 0
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style>
.fade-enter-active, .fade-leave-active { transition: opacity 0.16s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
