import request from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  nickname: string
  avatar: string
}

export interface UserDTO {
  id: number
  username: string
  nickname: string
  avatar: string
}

export function login(data: LoginRequest) {
  return request.post<any, { data: LoginResponse }>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return request.post('/auth/register', data)
}

export function getCurrentUser() {
  return request.get<any, { data: UserDTO }>('/auth/me')
}

export function updateUser(data: { nickname?: string; avatar?: string }) {
  return request.put('/auth/me', data)
}
