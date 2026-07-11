import request from './request'

export interface MonthlyStatsVO {
  year: number
  month: number
  income: number
  expense: number
  balance: number
  categoryBreakdown: Array<{
    categoryId: number
    categoryName: string
    amount: number
    percentage: number
  }>
}

export function getMonthlyStats(year: number, month: number) {
  return request.post<any, { data: MonthlyStatsVO }>('/statistics/monthly', { year, month })
}

export interface TrendVO {
  points: Array<{
    year: number
    month: number
    income: number
    expense: number
    balance: number
  }>
}

export function getTrend(months: number = 6) {
  return request.post<any, { data: TrendVO }>('/statistics/trend', { months })
}

export interface DailyVO {
  year: number
  month: number
  days: Array<{
    day: number
    income: number
    expense: number
  }>
}

export function getDaily(year: number, month: number) {
  return request.post<any, { data: DailyVO }>('/statistics/daily', { year, month })
}

export function exportExcel(year: number, month: number) {
  return request.post('/statistics/export-excel', { year, month }, { responseType: 'blob' })
}
