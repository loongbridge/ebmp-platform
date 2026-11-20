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

export const workflowApi = {
  list: (params: any) => http.get<any, any>('/workflow/approvals', { params }),
  todo: () => http.get<any, any>('/workflow/todo'),
  detail: (id: number) => http.get<any, any>(`/workflow/approvals/${id}`),
  approve: (id: number, comment?: string) => http.post<any, any>(`/workflow/approvals/${id}/approve`, { comment }),
  reject: (id: number, comment?: string) => http.post<any, any>(`/workflow/approvals/${id}/reject`, { comment }),
  stats: () => http.get<any, any>('/workflow/stats')
}

export const oaApi = {
  notices: (params: any) => http.get<any, any>('/oa/notices', { params }),
  tasks: (params: any) => http.get<any, any>('/oa/tasks', { params }),
  completeTask: (id: number) => http.post<any, any>(`/oa/tasks/${id}/complete`),
  attendance: (params: any) => http.get<any, any>('/oa/attendance', { params }),
  attendanceSummary: () => http.get<any, any>('/oa/attendance/summary'),
  checkIn: () => http.post<any, any>('/oa/attendance/check-in'),
  checkOut: () => http.post<any, any>('/oa/attendance/check-out')
}

export const erpApi = {
  stock: (params: any) => http.get<any, any>('/erp/stock', { params }),
  alerts: () => http.get<any, any>('/erp/stock/alerts'),
  stockValue: () => http.get<any, any>('/erp/stock/value')
}

export const crmApi = {
  leads: () => http.get<any, any>('/crm/leads'),
  convertLead: (id: number) => http.post<any, any>(`/crm/leads/${id}/convert`),
  stats: () => http.get<any, any>('/crm/stats'),
  opportunities: () => http.get<any, any>('/crm/opportunities'),
  activitiesByBiz: (bizType: string, bizId: number) => http.get<any, any>(`/crm/activities/${bizType}/${bizId}`)
}
