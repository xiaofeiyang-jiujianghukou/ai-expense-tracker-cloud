import request from './request'

export interface BillVO {
  id: number
  categoryId: number
  categoryName: string
  amount: number
  type: string
  description: string
  billDate: string
  createdTime: string
}

export interface BillListRequest {
  type?: string
  categoryId?: number
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export function listBills(params: BillListRequest) {
  return request.post<any, { data: PageResult<BillVO> }>('/bills/list', params)
}

export function createBill(data: {
  categoryId: number; amount: number; type: string
  description?: string; billDate: string
}) {
  return request.post('/bills', data)
}

export function updateBill(data: {
  id: number; categoryId: number; amount: number; type: string
  description?: string; billDate: string
}) {
  return request.post('/bills/update', data)
}

export function deleteBill(id: number) {
  return request.post('/bills/delete', { id })
}

export function exportBillsCsv(params: BillListRequest) {
  return request.post('/bills/export-csv', params, { responseType: 'blob' })
}
