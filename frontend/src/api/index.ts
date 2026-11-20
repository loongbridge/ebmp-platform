import http from './http'

export const authApi = {
  login: (data: { username: string; password: string }) => http.post<any, any>('/auth/login', data),
  me: () => http.get<any, any>('/auth/me'),
  logout: () => http.post<any, any>('/auth/logout')
}

export const dashboardApi = {
  overview: () => http.get<any, any>('/dashboard/overview'),
  todos: () => http.get<any, any>('/dashboard/todos')
}

export const systemApi = {
  users: (params: any) => http.get<any, any>('/system/users', { params }),
  userOptions: () => http.get<any, any>('/system/users/options'),
  createUser: (data: any) => http.post<any, any>('/system/users', data),
  updateUser: (id: number, data: any) => http.put<any, any>(`/system/users/${id}`, data),
  deleteUser: (id: number) => http.delete<any, any>(`/system/users/${id}`),
  roles: () => http.get<any, any>('/system/roles'),
  roleOptions: () => http.get<any, any>('/system/roles/options'),
  createRole: (data: any) => http.post<any, any>('/system/roles', data),
  updateRole: (id: number, data: any) => http.put<any, any>(`/system/roles/${id}`, data),
  deleteRole: (id: number) => http.delete<any, any>(`/system/roles/${id}`),
  permissions: () => http.get<any, any>('/system/permissions'),
  deptTree: () => http.get<any, any>('/system/depts/tree'),
  deptOptions: () => http.get<any, any>('/system/depts/options'),
  createDept: (data: any) => http.post<any, any>('/system/depts', data),
  updateDept: (id: number, data: any) => http.put<any, any>(`/system/depts/${id}`, data),
  deleteDept: (id: number) => http.delete<any, any>(`/system/depts/${id}`)
}

export const masterApi = {
  customers: (params: any) => http.get<any, any>('/master/customers', { params }),
  customerOptions: () => http.get<any, any>('/master/customers/options'),
  customer: (id: number) => http.get<any, any>(`/master/customers/${id}`),
  createCustomer: (data: any) => http.post<any, any>('/master/customers', data),
  updateCustomer: (id: number, data: any) => http.put<any, any>(`/master/customers/${id}`, data),
  deleteCustomer: (id: number) => http.delete<any, any>(`/master/customers/${id}`),
  contacts: (id: number) => http.get<any, any>(`/master/customers/${id}/contacts`),
  createContact: (data: any) => http.post<any, any>('/master/contacts', data),
  deleteContact: (id: number) => http.delete<any, any>(`/master/contacts/${id}`),
  suppliers: (params: any) => http.get<any, any>('/master/suppliers', { params }),
  supplierOptions: () => http.get<any, any>('/master/suppliers/options'),
  createSupplier: (data: any) => http.post<any, any>('/master/suppliers', data),
  updateSupplier: (id: number, data: any) => http.put<any, any>(`/master/suppliers/${id}`, data),
  deleteSupplier: (id: number) => http.delete<any, any>(`/master/suppliers/${id}`),
  materials: (params: any) => http.get<any, any>('/master/materials', { params }),
  materialOptions: () => http.get<any, any>('/master/materials/options'),
  createMaterial: (data: any) => http.post<any, any>('/master/materials', data),
  updateMaterial: (id: number, data: any) => http.put<any, any>(`/master/materials/${id}`, data),
  deleteMaterial: (id: number) => http.delete<any, any>(`/master/materials/${id}`),
  warehouses: () => http.get<any, any>('/master/warehouses'),
  createWarehouse: (data: any) => http.post<any, any>('/master/warehouses', data),
  updateWarehouse: (id: number, data: any) => http.put<any, any>(`/master/warehouses/${id}`, data),
  deleteWarehouse: (id: number) => http.delete<any, any>(`/master/warehouses/${id}`)
}

export const oaApi = {
  notices: (params: any) => http.get<any, any>('/oa/notices', { params }),
  createNotice: (data: any) => http.post<any, any>('/oa/notices', data),
  updateNotice: (id: number, data: any) => http.put<any, any>(`/oa/notices/${id}`, data),
  deleteNotice: (id: number) => http.delete<any, any>(`/oa/notices/${id}`),
  tasks: (params: any) => http.get<any, any>('/oa/tasks', { params }),
  createTask: (data: any) => http.post<any, any>('/oa/tasks', data),
  updateTask: (id: number, data: any) => http.put<any, any>(`/oa/tasks/${id}`, data),
  completeTask: (id: number) => http.post<any, any>(`/oa/tasks/${id}/complete`),
  deleteTask: (id: number) => http.delete<any, any>(`/oa/tasks/${id}`),
  schedules: (params: any) => http.get<any, any>('/oa/schedules', { params }),
  createSchedule: (data: any) => http.post<any, any>('/oa/schedules', data),
  updateSchedule: (id: number, data: any) => http.put<any, any>(`/oa/schedules/${id}`, data),
  deleteSchedule: (id: number) => http.delete<any, any>(`/oa/schedules/${id}`),
  meetings: () => http.get<any, any>('/oa/meetings'),
  createMeeting: (data: any) => http.post<any, any>('/oa/meetings', data),
  updateMeeting: (id: number, data: any) => http.put<any, any>(`/oa/meetings/${id}`, data),
  deleteMeeting: (id: number) => http.delete<any, any>(`/oa/meetings/${id}`),
  leaves: (params: any) => http.get<any, any>('/oa/leaves', { params }),
  createLeave: (data: any) => http.post<any, any>('/oa/leaves', data),
  submitLeave: (id: number) => http.post<any, any>(`/oa/leaves/${id}/submit`),
  attendance: (params: any) => http.get<any, any>('/oa/attendance', { params }),
  checkIn: () => http.post<any, any>('/oa/attendance/check-in'),
  checkOut: () => http.post<any, any>('/oa/attendance/check-out'),
  attendanceSummary: () => http.get<any, any>('/oa/attendance/summary'),
  documents: () => http.get<any, any>('/oa/documents'),
  createDocument: (data: any) => http.post<any, any>('/oa/documents', data),
  deleteDocument: (id: number) => http.delete<any, any>(`/oa/documents/${id}`)
}

export const srmApi = {
  requisitions: () => http.get<any, any>('/srm/requisitions'),
  requisition: (id: number) => http.get<any, any>(`/srm/requisitions/${id}`),
  createRequisition: (data: any) => http.post<any, any>('/srm/requisitions', data),
  submitRequisition: (id: number) => http.post<any, any>(`/srm/requisitions/${id}/submit`),
  orders: () => http.get<any, any>('/srm/orders'),
  order: (id: number) => http.get<any, any>(`/srm/orders/${id}`),
  createOrder: (data: any) => http.post<any, any>('/srm/orders', data),
  submitOrder: (id: number) => http.post<any, any>(`/srm/orders/${id}/submit`),
  receive: (id: number, warehouseId: number) => http.post<any, any>(`/srm/orders/${id}/receive`, { warehouseId }),
  evaluations: () => http.get<any, any>('/srm/evaluations'),
  createEvaluation: (data: any) => http.post<any, any>('/srm/evaluations', data),
  performance: () => http.get<any, any>('/srm/performance')
}

export const erpApi = {
  orders: () => http.get<any, any>('/erp/orders'),
  order: (id: number) => http.get<any, any>(`/erp/orders/${id}`),
  createOrder: (data: any) => http.post<any, any>('/erp/orders', data),
  submitOrder: (id: number) => http.post<any, any>(`/erp/orders/${id}/submit`),
  outbound: (id: number, warehouseId: number) => http.post<any, any>(`/erp/orders/${id}/outbound`, { warehouseId }),
  stock: (params: any) => http.get<any, any>('/erp/stock', { params }),
  alerts: () => http.get<any, any>('/erp/stock/alerts'),
  stockValue: () => http.get<any, any>('/erp/stock/value'),
  ledger: () => http.get<any, any>('/erp/stock/ledger'),
  adjust: (data: any) => http.post<any, any>('/erp/stock/adjust', data)
}

export const crmApi = {
  leads: () => http.get<any, any>('/crm/leads'),
  createLead: (data: any) => http.post<any, any>('/crm/leads', data),
  updateLead: (id: number, data: any) => http.put<any, any>(`/crm/leads/${id}`, data),
  convertLead: (id: number) => http.post<any, any>(`/crm/leads/${id}/convert`),
  deleteLead: (id: number) => http.delete<any, any>(`/crm/leads/${id}`),
  opportunities: () => http.get<any, any>('/crm/opportunities'),
  pipeline: () => http.get<any, any>('/crm/opportunities/pipeline'),
  createOpportunity: (data: any) => http.post<any, any>('/crm/opportunities', data),
  updateOpportunity: (id: number, data: any) => http.put<any, any>(`/crm/opportunities/${id}`, data),
  win: (id: number, items: any[]) => http.post<any, any>(`/crm/opportunities/${id}/win`, { items }),
  deleteOpportunity: (id: number) => http.delete<any, any>(`/crm/opportunities/${id}`),
  activities: () => http.get<any, any>('/crm/activities'),
  activitiesByBiz: (bizType: string, bizId: number) => http.get<any, any>(`/crm/activities/${bizType}/${bizId}`),
  createActivity: (data: any) => http.post<any, any>('/crm/activities', data),
  deleteActivity: (id: number) => http.delete<any, any>(`/crm/activities/${id}`),
  stats: () => http.get<any, any>('/crm/stats')
}

export const contractApi = {
  list: (params: any) => http.get<any, any>('/contracts', { params }),
  create: (data: any) => http.post<any, any>('/contracts', data),
  update: (id: number, data: any) => http.put<any, any>(`/contracts/${id}`, data),
  submit: (id: number) => http.post<any, any>(`/contracts/${id}/submit`),
  remove: (id: number) => http.delete<any, any>(`/contracts/${id}`)
}

export const workflowApi = {
  approvals: (params: any) => http.get<any, any>('/workflow/approvals', { params }),
  detail: (id: number) => http.get<any, any>(`/workflow/approvals/${id}`),
  approve: (id: number, comment?: string) => http.post<any, any>(`/workflow/approvals/${id}/approve`, { comment }),
  reject: (id: number, comment?: string) => http.post<any, any>(`/workflow/approvals/${id}/reject`, { comment }),
  todo: () => http.get<any, any>('/workflow/todo'),
  stats: () => http.get<any, any>('/workflow/stats')
}
