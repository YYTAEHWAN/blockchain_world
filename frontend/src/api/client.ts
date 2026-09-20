import axios from 'axios'

/**
 * 백엔드 API 클라이언트.
 * - baseURL 은 /api (vite proxy 가 http://localhost:8080 으로 전달)
 * - 요청마다 sessionStorage 의 JWT 를 Authorization 헤더로 자동 첨부
 * - 401 응답 시 토큰을 비우고 로그인으로 유도
 *
 * 토큰은 sessionStorage 에 저장한다 → 탭(창)마다 세션이 독립되어
 * 여러 탭에서 서로 다른 계정(어드민/사용자1/사용자2)으로 동시 로그인할 수 있다.
 * (같은 탭 새로고침 시 유지, 탭을 닫으면 사라짐)
 */
const TOKEN_KEY = 'rwa_token'

export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) sessionStorage.setItem(TOKEN_KEY, token)
  else sessionStorage.removeItem(TOKEN_KEY)
}

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error?.response?.status === 401) {
      setToken(null)
      // 이미 로그인 페이지가 아니면 이동
      if (!location.pathname.startsWith('/login')) {
        location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

/** 백엔드 표준 오류(message)에서 사람이 읽을 메시지를 뽑는다. */
export function errorMessage(e: unknown): string {
  if (axios.isAxiosError(e)) {
    return e.response?.data?.message ?? e.message ?? '요청에 실패했습니다'
  }
  return '요청에 실패했습니다'
}
