import request from './request'

export interface BudgetVO {
  id: number
  categoryId: number
  categoryName: string
  year: number
  month: number
  targetAmount: number
  actualAmount: number
  percentage: number
}

export interface BudgetListRequest {
  year: number
  month: number
}

export interface BudgetSetRequest {
  categoryId: number
  year: number
  month: number
  targetAmount: number
}

export function listBudgets(params: BudgetListRequest) {
  return request.post<any, { data: BudgetVO[] }>('/budgets/list', params)
}

export function setBudget(data: BudgetSetRequest) {
  return request.post('/budgets/set', data)
}
