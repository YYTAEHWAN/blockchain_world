import { Navigate, Route, Routes, Link, NavLink, useLocation } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import { PropertiesPage } from './pages/PropertiesPage'
import { KycPage } from './pages/KycPage'
import { PortfolioPage } from './pages/PortfolioPage'
import { DistributionsPage } from './pages/DistributionsPage'
import { AdminKycPage } from './pages/admin/AdminKycPage'
import { AdminPropertyPage } from './pages/admin/AdminPropertyPage'
import { AdminDistributionPage } from './pages/admin/AdminDistributionPage'
import { AdminMonitoringPage } from './pages/admin/AdminMonitoringPage'
import { AdminActionsPage } from './pages/admin/AdminActionsPage'
import type { ReactNode } from 'react'

/** 로그인 필요 라우트 */
function RequireAuth({ children }: { children: ReactNode }) {
  const { user, initializing } = useAuth()
  const location = useLocation()
  if (initializing) return <p className="app-main">불러오는 중...</p>
  if (!user) return <Navigate to="/login" state={{ from: location }} replace />
  return <>{children}</>
}

/** 관리자 전용 라우트 */
function RequireAdmin({ children }: { children: ReactNode }) {
  const { user, isAdmin, initializing } = useAuth()
  if (initializing) return <p className="app-main">불러오는 중...</p>
  if (!user) return <Navigate to="/login" replace />
  if (!isAdmin) return <Navigate to="/" replace />
  return <>{children}</>
}

/** 경로 → 화면 제목 매핑 (배너에 현재 위치 표시) */
const PAGE_TITLES: Record<string, string> = {
  '/': '부동산 목록',
  '/login': '로그인',
  '/signup': '회원가입',
  '/kyc': 'KYC 인증',
  '/portfolio': '내 포트폴리오',
  '/distributions': '내 배당 내역',
  '/admin/kyc': 'KYC 심사',
  '/admin/properties': '부동산 등록 · 토큰 발행',
  '/admin/distributions': '배당 집행',
  '/admin/monitoring': '모니터링 대시보드',
  '/admin/actions': '조치 이력',
}

function Header() {
  const { user, isAdmin, logout } = useAuth()
  return (
    <header className="app-header">
      <Link to="/" className="brand">한화 RWA 부동산</Link>
      <nav>
        <NavLink to="/" end>부동산</NavLink>
        {user && !isAdmin && (
          <>
            <NavLink to="/kyc">KYC</NavLink>
            <NavLink to="/portfolio">포트폴리오</NavLink>
            <NavLink to="/distributions">배당내역</NavLink>
          </>
        )}
        {isAdmin && (
          <>
            <NavLink to="/admin/kyc">KYC심사</NavLink>
            <NavLink to="/admin/properties">부동산등록</NavLink>
            <NavLink to="/admin/distributions">배당집행</NavLink>
            <NavLink to="/admin/monitoring">모니터링</NavLink>
            <NavLink to="/admin/actions">조치이력</NavLink>
          </>
        )}
      </nav>
      <div className="header-right">
        {user ? (
          <>
            <span className="user-badge">{user.email} ({user.role})</span>
            <button onClick={logout}>로그아웃</button>
          </>
        ) : (
          <>
            <NavLink to="/login">로그인</NavLink>
            <NavLink to="/signup">회원가입</NavLink>
          </>
        )}
      </div>
    </header>
  )
}

/** 현재 페이지 제목 배너 */
function PageBanner() {
  const location = useLocation()
  const title = PAGE_TITLES[location.pathname] ?? ''
  if (!title) return null
  return (
    <div className="page-banner">
      <span className="page-banner-title">{title}</span>
    </div>
  )
}

export function App() {
  return (
    <div className="app">
      <Header />
      <PageBanner />
      <main className="app-main">
        <Routes>
          <Route path="/" element={<PropertiesPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          <Route path="/kyc" element={<RequireAuth><KycPage /></RequireAuth>} />
          <Route path="/portfolio" element={<RequireAuth><PortfolioPage /></RequireAuth>} />
          <Route path="/distributions" element={<RequireAuth><DistributionsPage /></RequireAuth>} />

          <Route path="/admin/kyc" element={<RequireAdmin><AdminKycPage /></RequireAdmin>} />
          <Route path="/admin/properties" element={<RequireAdmin><AdminPropertyPage /></RequireAdmin>} />
          <Route path="/admin/distributions" element={<RequireAdmin><AdminDistributionPage /></RequireAdmin>} />
          <Route path="/admin/monitoring" element={<RequireAdmin><AdminMonitoringPage /></RequireAdmin>} />
          <Route path="/admin/actions" element={<RequireAdmin><AdminActionsPage /></RequireAdmin>} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}
