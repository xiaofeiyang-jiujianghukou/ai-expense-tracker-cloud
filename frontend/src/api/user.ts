import request from './request'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  nickname?: string
}

export interface UserVO {
  id: number
  email: string
  nickname: string
  createdTime: string
}

export interface LoginResponse {
  token: string
  user: UserVO
}

export function login(data: LoginRequest) {
  return request.post<any, { data: LoginResponse }>('/users/login', data)
}

export function register(data: RegisterRequest) {
  return request.post<any, { data: UserVO }>('/users/register', data)
}
