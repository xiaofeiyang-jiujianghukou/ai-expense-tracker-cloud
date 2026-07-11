import request from './request'

export interface CategoryVO {
  id: number
  name: string
  type: string
  createdTime: string
}

export function listCategories(type?: string) {
  return request.post<any, { data: CategoryVO[] }>('/categories/list', { type })
}

export function createCategory(data: { name: string; type: string }) {
  return request.post('/categories', data)
}

export function updateCategory(data: { id: number; name: string; type: string }) {
  return request.post('/categories/update', data)
}

export function deleteCategory(id: number) {
  return request.post('/categories/delete', { id })
}
