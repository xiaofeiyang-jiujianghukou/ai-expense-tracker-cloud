import request from './request'

export interface CategorizeRequest {
  description: string
  amount: number
  type: string  // 'INCOME' | 'EXPENSE'
}

export interface CategorizeResponse {
  categoryId: number
  categoryName: string
  confidence: number
  reason: string
}

export interface AnalysisRequest {
  year: number
  month: number
}

export interface AnalysisResponse {
  year: number
  month: number
  insights: string[]
}

export function categorize(data: CategorizeRequest) {
  return request.post<any, { data: CategorizeResponse }>('/ai/categorize', data)
}

export function getAnalysis(data: AnalysisRequest) {
  return request.post<any, { data: AnalysisResponse }>('/ai/analysis', data)
}

export function getReport(data: { year: number; month: number }) {
  return request.post<any, { data: { year: number; month: number; report: string } }>('/ai/report', data)
}

export interface BudgetAdviceItem {
  categoryName: string
  amount: number
}

export interface BudgetAdviceResponse {
  year: number
  month: number
  items: BudgetAdviceItem[]
}

export function getBudgetAdvice(data: AnalysisRequest) {
  return request.post<any, { data: BudgetAdviceResponse }>('/ai/budget-advice', data)
}

export function getAnomaly(data: AnalysisRequest) {
  return request.post<any, { data: AnalysisResponse }>('/ai/anomaly', data)
}
