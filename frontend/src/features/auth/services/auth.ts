import api from '@/services/api'

export type UserRole = 'ROLE_ADMIN' | 'ROLE_INSTRUCTOR' | 'ROLE_LEARNER'
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'

export interface AuthUser {
  userId: string
  email: string
  username: string
  firstName: string | null
  lastName: string | null
  status: UserStatus
  role: UserRole
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expireAt: string
  user: AuthUser
}

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  username: string
  firstName?: string
  lastName?: string
  role?: UserRole
}

const ACCESS_TOKEN_KEY = 'token'
const REFRESH_TOKEN_KEY = 'refreshToken'
const AUTH_USER_KEY = 'authUser'

export function saveAuthSession(auth: AuthResponse) {
  localStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken)
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(auth.user))
}

export async function login(payload: LoginPayload) {
  const { data } = await api.post<AuthResponse>('/v1/auth/login', payload)
  saveAuthSession(data)
  return data
}

export async function register(payload: RegisterPayload) {
  const { data } = await api.post<AuthResponse>('/v1/auth/register', payload)
  saveAuthSession(data)
  return data
}
