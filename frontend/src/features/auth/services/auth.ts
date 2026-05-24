import api from '@/services/api'

export type UserRole = 'ROLE_ADMIN' | 'ROLE_TEACHER' | 'ROLE_LEARNER'
export type UserStatus = 'VERIFY' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'

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

export interface MessageResponse {
  message: string
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
  const { data } = await api.post<MessageResponse>('/v1/auth/register', payload)
  return data
}

export async function requestEmailConfirmation(email: string) {
  const { data } = await api.post<MessageResponse>('/v1/auth/request-email-confirmation', { email })
  return data
}

export async function verifyEmail(token: string) {
  const { data } = await api.post<MessageResponse>('/v1/auth/verify-email', { token })
  return data
}

export async function forgotPassword(email: string) {
  const { data } = await api.post<MessageResponse>('/v1/auth/forgot-password', { email })
  return data
}

export async function resetPassword(token: string, newPassword: string) {
  const { data } = await api.post<MessageResponse>('/v1/auth/reset-password', {
    token,
    newPassword
  })
  return data
}
