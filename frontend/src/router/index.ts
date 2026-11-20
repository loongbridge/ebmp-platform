import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/login/Login.vue'), meta: { title: '登录' } },
  {
    path: '/',
    component: () => import('../layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/dashboard/Dashboard.vue'), meta: { title: '经营驾驶舱' } },
      { path: 'workflow/todo', component: () => import('../views/workflow/TodoCenter.vue'), meta: { title: '统一待办' } },
      { path: 'workflow/approvals', component: () => import('../views/workflow/ApprovalCenter.vue'), meta: { title: '审批中心' } },

      { path: 'oa/notices', component: () => import('../views/oa/NoticeList.vue'), meta: { title: '公告管理' } },
      { path: 'oa/tasks', component: () => import('../views/oa/TaskList.vue'), meta: { title: '任务管理' } },
      { path: 'oa/schedules', component: () => import('../views/oa/ScheduleList.vue'), meta: { title: '日程安排' } },
      { path: 'oa/meetings', component: () => import('../views/oa/MeetingList.vue'), meta: { title: '会议管理' } },
      { path: 'oa/leaves', component: () => import('../views/oa/LeaveList.vue'), meta: { title: '请假管理' } },
      { path: 'oa/attendance', component: () => import('../views/oa/Attendance.vue'), meta: { title: '考勤打卡' } },
      { path: 'oa/documents', component: () => import('../views/oa/DocumentList.vue'), meta: { title: '文档中心' } },

      { path: 'crm/leads', component: () => import('../views/crm/LeadList.vue'), meta: { title: '线索管理' } },
      { path: 'crm/opportunities', component: () => import('../views/crm/OpportunityList.vue'), meta: { title: '商机管理' } },
      { path: 'crm/customers', component: () => import('../views/crm/Customer360.vue'), meta: { title: '客户 360' } },
      { path: 'crm/activities', component: () => import('../views/crm/ActivityList.vue'), meta: { title: '跟进记录' } },

      { path: 'srm/requisitions', component: () => import('../views/srm/RequisitionList.vue'), meta: { title: '采购需求' } },
      { path: 'srm/orders', component: () => import('../views/srm/PurchaseOrderList.vue'), meta: { title: '采购订单' } },
      { path: 'srm/evaluations', component: () => import('../views/srm/EvaluationList.vue'), meta: { title: '供应商评价' } },
      { path: 'srm/performance', component: () => import('../views/srm/Performance.vue'), meta: { title: '供应商绩效' } },

      { path: 'erp/orders', component: () => import('../views/erp/SalesOrderList.vue'), meta: { title: '销售订单' } },
      { path: 'erp/stock', component: () => import('../views/erp/StockList.vue'), meta: { title: '库存查询' } },
      { path: 'erp/stock-alerts', component: () => import('../views/erp/StockAlert.vue'), meta: { title: '库存预警' } },
      { path: 'erp/ledger', component: () => import('../views/erp/StockLedger.vue'), meta: { title: '出入库台账' } },

      { path: 'master/customers', component: () => import('../views/masterdata/CustomerList.vue'), meta: { title: '客户档案' } },
      { path: 'master/suppliers', component: () => import('../views/masterdata/SupplierList.vue'), meta: { title: '供应商档案' } },
      { path: 'master/materials', component: () => import('../views/masterdata/MaterialList.vue'), meta: { title: '物料档案' } },
      { path: 'master/warehouses', component: () => import('../views/masterdata/WarehouseList.vue'), meta: { title: '仓库档案' } },

      { path: 'contracts', component: () => import('../views/contract/ContractList.vue'), meta: { title: '合同中心' } },

      { path: 'system/users', component: () => import('../views/system/UserList.vue'), meta: { title: '用户管理' } },
      { path: 'system/roles', component: () => import('../views/system/RoleList.vue'), meta: { title: '角色权限' } },
      { path: 'system/depts', component: () => import('../views/system/DeptList.vue'), meta: { title: '部门管理' } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '企业商业管理平台'
  if (to.path === '/login') return true
  if (!localStorage.getItem('ebmp_token')) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
