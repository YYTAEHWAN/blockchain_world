import { useState, useEffect } from 'react'
import { api, errorMessage } from '../../api/client'

interface Investor {
  id: number
  email: string
  role: 'INVESTOR' | 'ADMIN'
  walletAddress: string | null
  createdAt: string
}

export function AdminInvestorPage() {
  const [investors, setInvestors] = useState<Investor[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState<'ALL' | 'INVESTOR' | 'ADMIN'>('ALL')

  const loadInvestors = async () => {
    setLoading(true)
    setError(null)
    
    try {
      // 쿼리 파라미터 구성
      const params = new URLSearchParams()
      if (search.trim()) {
        params.append('search', search.trim())
      }
      if (roleFilter !== 'ALL') {
        params.append('role', roleFilter)
      }
      
      const url = `/admin/investors${params.toString() ? '?' + params.toString() : ''}`
      const response = await api.get<Investor[]>(url)
      setInvestors(response.data)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadInvestors()
  }, [])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    loadInvestors()
  }

  const handleRoleFilter = (role: 'ALL' | 'INVESTOR' | 'ADMIN') => {
    setRoleFilter(role)
    // 필터 변경 시 즉시 검색
    setTimeout(() => loadInvestors(), 0)
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleDateString('ko-KR') + ' ' + date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  return (
    <div>
      <h2>투자자 관리</h2>
      <p className="hint">가입된 모든 투자자 및 관리자 계정을 확인하고 관리할 수 있습니다.</p>

      {/* 검색 및 필터 섹션 */}
      <div className="card" style={{ marginBottom: '20px' }}>
        <div className="two-col">
          <div>
            <h3>검색</h3>
            <form onSubmit={handleSearch} style={{ display: 'flex', gap: '10px' }}>
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="이메일 검색..."
                style={{ flex: 1 }}
              />
              <button type="submit" disabled={loading}>검색</button>
              <button type="button" onClick={() => { setSearch(''); loadInvestors() }} disabled={loading}>
                초기화
              </button>
            </form>
          </div>
          <div>
            <h3>역할 필터</h3>
            <div className="btn-row">
              <button 
                onClick={() => handleRoleFilter('ALL')}
                className={roleFilter === 'ALL' ? 'primary' : ''}
                disabled={loading}
              >
                전체
              </button>
              <button 
                onClick={() => handleRoleFilter('INVESTOR')}
                className={roleFilter === 'INVESTOR' ? 'primary' : ''}
                disabled={loading}
              >
                투자자
              </button>
              <button 
                onClick={() => handleRoleFilter('ADMIN')}
                className={roleFilter === 'ADMIN' ? 'primary' : ''}
                disabled={loading}
              >
                관리자
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* 오류 메시지 */}
      {error && <p className="error">{error}</p>}

      {/* 투자자 목록 */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3>투자자 목록</h3>
          <button onClick={loadInvestors} disabled={loading}>
            {loading ? '불러오는 중...' : '새로고침'}
          </button>
        </div>

        {loading && investors.length === 0 ? (
          <p>불러오는 중...</p>
        ) : investors.length === 0 ? (
          <p className="hint">검색 결과가 없습니다.</p>
        ) : (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>이메일</th>
                  <th>역할</th>
                  <th>지갑 주소</th>
                  <th>가입일</th>
                </tr>
              </thead>
              <tbody>
                {investors.map((investor) => (
                  <tr key={investor.id}>
                    <td>{investor.id}</td>
                    <td>{investor.email}</td>
                    <td>
                      <span className={`badge ${investor.role === 'ADMIN' ? 'badge-admin' : 'badge-investor'}`}>
                        {investor.role === 'ADMIN' ? '관리자' : '투자자'}
                      </span>
                    </td>
                    <td style={{ fontFamily: 'monospace', fontSize: '12px' }}>
                      {investor.walletAddress ? `${investor.walletAddress.slice(0, 8)}...${investor.walletAddress.slice(-6)}` : '미등록'}
                    </td>
                    <td>{formatDate(investor.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div style={{ marginTop: '16px', color: '#666', fontSize: '14px' }}>
          총 {investors.length}명의 계정이 있습니다.
        </div>
      </div>

      <div className="card">
        <h3>통계 정보</h3>
        <div className="two-col">
          <div>
            <h4>역할별 계정 수</h4>
            <dl className="kv">
              <div>
                <dt>총 계정 수</dt>
                <dd>{investors.length}명</dd>
              </div>
              <div>
                <dt>투자자</dt>
                <dd>{investors.filter(i => i.role === 'INVESTOR').length}명</dd>
              </div>
              <div>
                <dt>관리자</dt>
                <dd>{investors.filter(i => i.role === 'ADMIN').length}명</dd>
              </div>
            </dl>
          </div>
          <div>
            <h4>지갑 등록 현황</h4>
            <dl className="kv">
              <div>
                <dt>지갑 등록 계정</dt>
                <dd>{investors.filter(i => i.walletAddress).length}명</dd>
              </div>
              <div>
                <dt>지갑 미등록 계정</dt>
                <dd>{investors.filter(i => !i.walletAddress).length}명</dd>
              </div>
              <div>
                <dt>등록률</dt>
                <dd>
                  {investors.length > 0 
                    ? Math.round((investors.filter(i => i.walletAddress).length / investors.length) * 100) 
                    : 0}%
                </dd>
              </div>
            </dl>
          </div>
        </div>
      </div>
    </div>
  )
}