import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, getToken, setToken } from '../api/client'

/** JWT payload 에서 꺼낸 최소 사용자 정보 */
export interface AuthUser {
  email: string
  role: 'INVESTOR' | 'ADMIN'
}

interface AuthContextValue {
  user: AuthUser | null
  isAdmin: boolean
  /** 저장된 토큰으로 사용자 복원 중인지 (true 동안은 보호 라우트가 리다이렉트하지 않음) */
  initializing: boolean
  login: (email: string, password: string) => Promise<void>
  signup: (email: string, password: string, walletAddress?: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

/** JWT(base64url) 페이로드를 디코드한다. 만료된 토큰은 null 반환. */
function decodeToken(token: string): AuthUser | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    // 만료 검사 (exp 는 초 단위)
    if (payload.exp && Date.now() >= payload.exp * 1000) return null
    const role = payload.role as AuthUser['role']
    const email = (payload.sub ?? payload.email) as string
    if (!email || !role) return null
    return { email, role }
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [initializing, setInitializing] = useState(true)

  // 새로고침 시 저장된 토큰으로 사용자 복원
  useEffect(() => {
    const token = getToken()
    if (token) {
      const restored = decodeToken(token)
      if (restored) setUser(restored)
      else setToken(null) // 만료/손상 토큰 제거
    }
    setInitializing(false)
  }, [])

  const applyToken = (token: string) => {
    setToken(token)
    setUser(decodeToken(token))
  }

  const login = async (email: string, password: string) => {
    const { data } = await api.post('/auth/login', { email, password })
    applyToken(data.token)
  }

  const signup = async (email: string, password: string, walletAddress?: string) => {
    const { data } = await api.post('/auth/signup', { email, password, walletAddress })
    applyToken(data.token)
  }

  const logout = () => {
    setToken(null)
    setUser(null)
  }

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAdmin: user?.role === 'ADMIN', initializing, login, signup, logout }),
    [user, initializing],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
